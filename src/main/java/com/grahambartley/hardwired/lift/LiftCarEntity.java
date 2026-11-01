package com.grahambartley.hardwired.lift;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public final class LiftCarEntity extends Entity {

  public static final float PLATE = 0.125F;

  private static final double COLUMN_CENTER = 0.5;
  private static final double STANDING_REACH = 0.25;
  private static final int ELAPSED_TICKS = 20;
  private static final String HEAD = "head";
  private static final String TURNS = "turns";

  private static final TrackedData<Integer> TRIP_SERIAL =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.INTEGER);
  private static final TrackedData<Float> TRIP_FROM =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.FLOAT);
  private static final TrackedData<Float> TRIP_TO =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.FLOAT);
  private static final TrackedData<Integer> TRIP_PACE =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.INTEGER);
  private static final TrackedData<Integer> TRIP_ELAPSED =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.INTEGER);

  private static final TrackedData<Integer> QUARTER_TURNS =
      DataTracker.registerData(LiftCarEntity.class, TrackedDataHandlerRegistry.INTEGER);

  private final LiftRun run = new LiftRun();
  private final LiftMotion motion = new LiftMotion();
  private BlockPos head;

  public LiftCarEntity(EntityType<? extends LiftCarEntity> type, World world) {
    super(type, world);
    noClip = true;
  }

  public void placeInShaft(BlockPos liftHead, int level) {
    head = liftHead.toImmutable();
    run.placeAt(level);
    moveToLevel(level);
  }

  public BlockPos head() {
    return head;
  }

  public int quarterTurns() {
    return dataTracker.get(QUARTER_TURNS);
  }

  public void faceGate(Direction gateFacing) {
    dataTracker.set(QUARTER_TURNS, LiftCarTurn.quarterTurns(gateFacing));
  }

  public LiftRun run() {
    return run;
  }

  public int span() {
    return Math.round(getWidth());
  }

  public void moveToLevel(double level) {
    setPosition(columnX(), level - PLATE, columnZ());
  }

  private double columnX() {
    return head.getX() + COLUMN_CENTER * span();
  }

  private double columnZ() {
    return head.getZ() + COLUMN_CENTER * span();
  }

  public LiftDriver driver(ServerWorld world) {
    return new LiftDriver(this, run, shaft(world));
  }

  private LiftShaft shaft(ServerWorld world) {
    return new LiftShaft(world, head, span());
  }

  public void tripChanged() {
    LiftTrip trip = run.trip();
    dataTracker.set(TRIP_FROM, trip == null ? 0F : (float) trip.fromLevel());
    dataTracker.set(TRIP_TO, trip == null ? 0F : (float) trip.toLevel());
    dataTracker.set(TRIP_PACE, trip == null ? 0 : trip.ticksPerBlock());
    dataTracker.set(TRIP_ELAPSED, run.ticks());
    dataTracker.set(TRIP_SERIAL, run.serial());
  }

  public void keepTripInStep() {
    if (run.ticks() % ELAPSED_TICKS == 0) {
      dataTracker.set(TRIP_ELAPSED, run.ticks());
    }
  }

  @Override
  protected void initDataTracker(DataTracker.Builder builder) {
    builder.add(TRIP_SERIAL, 0);
    builder.add(TRIP_FROM, 0F);
    builder.add(TRIP_TO, 0F);
    builder.add(TRIP_PACE, 0);
    builder.add(TRIP_ELAPSED, 0);
    builder.add(QUARTER_TURNS, 0);
  }

  @Override
  public void tick() {
    super.tick();
    if (getWorld() instanceof ServerWorld world) {
      if (head == null) {
        discard();
      } else {
        driver(world).tick();
      }
    } else {
      followTrip();
    }
  }

  private void followTrip() {
    int pace = dataTracker.get(TRIP_PACE);
    LiftTrip trip =
        pace == 0 ? null : new LiftTrip(dataTracker.get(TRIP_FROM), dataTracker.get(TRIP_TO), pace);
    motion.sync(dataTracker.get(TRIP_SERIAL), trip, dataTracker.get(TRIP_ELAPSED));
    setPosition(getX(), motion.next(getY() + PLATE) - PLATE, getZ());
  }

  @Override
  public void updateTrackedPositionAndAngles(
      double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
    motion.hear(y + PLATE);
  }

  @Override
  public void remove(RemovalReason reason) {
    if (reason.shouldDestroy() && head != null && getWorld() instanceof ServerWorld world) {
      driver(world).abandon();
    }
    super.remove(reason);
  }

  @Override
  public boolean isCollidable() {
    return true;
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  public boolean isPushedByFluids() {
    return false;
  }

  @Override
  public boolean canHit() {
    return false;
  }

  @Override
  public boolean damage(DamageSource source, float amount) {
    return false;
  }

  @Override
  public boolean hasNoGravity() {
    return true;
  }

  @Override
  public boolean hasPlayerRider() {
    return false;
  }

  @Override
  protected boolean canAddPassenger(Entity passenger) {
    return passenger instanceof PlayerEntity;
  }

  @Override
  protected void updatePassengerPosition(Entity passenger, PositionUpdater updater) {
    int seat = getPassengerList().indexOf(passenger);
    updater.accept(
        passenger,
        getX() + LiftSafety.seatX(seat) * span(),
        getY() + PLATE,
        getZ() + LiftSafety.seatZ(seat) * span());
  }

  @Override
  public Vec3d updatePassengerForDismount(LivingEntity passenger) {
    if (head != null && getWorld() instanceof ServerWorld world) {
      if (run.phase() == LiftPhase.RESTING) {
        return new Vec3d(passenger.getX(), run.level(), passenger.getZ());
      }
      return LiftRiders.nearestGate(shaft(world).gates(), passenger.getY())
          .map(LiftRiders::spotAt)
          .orElseGet(passenger::getPos);
    }
    return passenger.getPos();
  }

  @Override
  public Box getVisibilityBoundingBox() {
    return getBoundingBox().stretch(0, LiftCarWalls.REACH_ABOVE_FLOOR, 0);
  }

  @Override
  protected void writeCustomDataToNbt(NbtCompound nbt) {
    if (head != null) {
      nbt.putLong(HEAD, head.asLong());
    }
    nbt.putInt(TURNS, quarterTurns());
    run.write(nbt);
  }

  @Override
  protected void readCustomDataFromNbt(NbtCompound nbt) {
    if (nbt.contains(HEAD)) {
      head = BlockPos.fromLong(nbt.getLong(HEAD));
    }
    dataTracker.set(QUARTER_TURNS, nbt.getInt(TURNS));
    run.read(nbt);
    tripChanged();
  }

  public List<Box> walls() {
    return LiftCarWalls.boxes(getX(), getY() + PLATE, getZ(), span(), quarterTurns());
  }

  public static List<VoxelShape> withWalls(Entity mover, Box path, List<VoxelShape> shapes) {
    if (mover instanceof LiftCarEntity || mover.getVehicle() instanceof LiftCarEntity) {
      return shapes;
    }
    Box reach = path.stretch(0, -LiftCarWalls.REACH_ABOVE_FLOOR, 0);
    List<LiftCarEntity> cars =
        mover.getWorld().getEntitiesByClass(LiftCarEntity.class, reach, car -> true);
    if (cars.isEmpty()) {
      return shapes;
    }
    List<VoxelShape> all = new ArrayList<>(shapes);
    cars.forEach(car -> car.walls().forEach(wall -> all.add(VoxelShapes.cuboid(wall))));
    return all;
  }

  public static boolean holdsUp(Entity entity) {
    Box below = entity.getBoundingBox().stretch(0, -STANDING_REACH, 0);
    return !entity.getWorld().getEntitiesByClass(LiftCarEntity.class, below, car -> true).isEmpty();
  }

  public static void riderLeaving(ServerPlayerEntity player) {
    if (player.getVehicle() instanceof LiftCarEntity car
        && car.head != null
        && car.getWorld() instanceof ServerWorld world) {
      car.driver(world).release(player);
    }
  }
}
