package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.lift.LiftActions;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftPhase;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.network.LiftChoosePayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModEntities;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

public class LiftGameTest implements FabricGameTest {

  private static final String SHAFT = "hardwired:shaft";
  private static final int FLOOR_Y = -1;
  private static final int LOW = 0;
  private static final int MIDDLE = 3;
  private static final int HIGH = 6;
  private static final int COLUMN_X = 3;
  private static final int COLUMN_Z = 3;
  private static final int GATE_X = 2;
  private static final int PILLAR_X = 1;
  private static final int HEAD_Y = 9;
  private static final int FULL_CHARGE = 100;
  private static final int START_TICK = 5;
  private static final int TICKS_FOR_SIX_BLOCKS = 36;
  private static final int SIX_BLOCK_RIDE_TICKS = 60;
  private static final int RIDE_TICK_LIMIT = 240;
  private static final Identifier GOING_UP = Identifier.of(Hardwired.MOD_ID, "going_up");
  private static final BlockPos CELL = new BlockPos(PILLAR_X, FLOOR_Y + 1, 2);
  private static final BlockPos HEAD = new BlockPos(COLUMN_X, HEAD_Y, COLUMN_Z);

  private static BlockPos gate(int level) {
    return new BlockPos(GATE_X, level, COLUMN_Z);
  }

  private static BlockPos inCar(int level) {
    return new BlockPos(COLUMN_X, level, COLUMN_Z);
  }

  private static void wire(WireEngine engine, BlockPos pos, Direction side, TestContext context) {
    engine.lay(new WireCell(new WireFace(context.getAbsolutePos(pos), side), Hue.PLAIN));
  }

  private static void placeGate(TestContext context, int level) {
    BlockState lower =
        ModBlocks.LIFT_FLOOR
            .getDefaultState()
            .with(LiftFloorBlock.FACING, Direction.EAST)
            .with(LiftFloorBlock.HALF, DoubleBlockHalf.LOWER);
    context.setBlockState(gate(level), lower);
    context.setBlockState(gate(level).up(), lower.with(LiftFloorBlock.HALF, DoubleBlockHalf.UPPER));
  }

  private static RedstoneCellBlockEntity build(TestContext context, int charge) {
    for (int y = FLOOR_Y + 1; y <= HEAD_Y; y++) {
      context.setBlockState(new BlockPos(PILLAR_X, y, COLUMN_Z), Blocks.STONE);
    }
    context.setBlockState(gate(MIDDLE).down(), Blocks.STONE);
    context.setBlockState(gate(HIGH).down(), Blocks.STONE);
    context.setBlockState(new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z), Blocks.STONE);
    placeGate(context, LOW);
    placeGate(context, MIDDLE);
    placeGate(context, HIGH);
    context.setBlockState(HEAD, ModBlocks.LIFT);
    context.setBlockState(CELL, ModBlocks.REDSTONE_CELL);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int y = FLOOR_Y + 1; y <= HEAD_Y; y++) {
      wire(engine, new BlockPos(PILLAR_X, y, COLUMN_Z), Direction.EAST, context);
    }
    wire(engine, new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z), Direction.UP, context);
    wire(engine, HEAD, Direction.WEST, context);
    wire(engine, new BlockPos(GATE_X, FLOOR_Y, COLUMN_Z), Direction.UP, context);
    wire(engine, new BlockPos(GATE_X, FLOOR_Y, COLUMN_Z - 1), Direction.UP, context);
    wire(engine, new BlockPos(PILLAR_X, FLOOR_Y, COLUMN_Z - 1), Direction.UP, context);
    RedstoneCellBlockEntity cell = context.getBlockEntity(CELL);
    cell.setCharge(charge);
    return cell;
  }

  private static void useGate(TestContext context, ConnectedTestPlayer player, int level) {
    LiftActions.use(player.player(), context.getAbsolutePos(gate(level)));
  }

  private static void choose(TestContext context, ConnectedTestPlayer player, int from, int to) {
    LiftActions.choose(
        player.player(),
        new LiftChoosePayload(
            context.getAbsolutePos(gate(from)), context.getAbsolutePos(gate(to)).getY()));
  }

  private static LiftShaft shaft(TestContext context) {
    return new LiftShaft(context.getWorld(), context.getAbsolutePos(HEAD));
  }

  private static LiftCarEntity car(TestContext context) {
    return shaft(context).car().orElseThrow(() -> new AssertionError("no car"));
  }

  private static boolean isOpen(TestContext context, int level) {
    return context.getBlockState(gate(level)).get(LiftFloorBlock.OPEN);
  }

  private static double carLevel(TestContext context, LiftCarEntity car) {
    return car.run().level() - context.getAbsolutePos(BlockPos.ORIGIN).getY();
  }

  private static void expectAtGate(
      TestContext context, ConnectedTestPlayer rider, int level, String name) {
    ServerPlayerEntity player = rider.player();
    BlockPos spot = context.getAbsolutePos(gate(level));
    context.assertFalse(player.hasVehicle(), name + " still riding");
    context.assertTrue(player.getBlockPos().equals(spot), name + " at " + player.getBlockPos());
    context.assertTrue(
        MathHelper.approximatelyEquals(player.getY(), spot.getY()),
        name + " feet " + player.getY());
    context.assertFalse(player.isInsideWall(), name + " in a wall");
  }

  private static ConnectedTestPlayer riderFrom(TestContext context, int level) {
    return ConnectedTestPlayer.joinAt(context, inCar(level));
  }

  private static void finish(TestContext context, ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
    context.complete();
  }

  private static void openAt(TestContext context, ConnectedTestPlayer rider, int level) {
    useGate(context, rider, level);
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aRideEndsAtTheChosenFloorFacingOut(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.assertTrue(isOpen(context, LOW), "the car opens the gate it stands at");
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.assertTrue(rider.player().hasVehicle(), "rider mounted");
          context.assertFalse(isOpen(context, LOW), "gate closed behind the car");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectAtGate(context, rider, HIGH, "rider");
          context.assertTrue(isOpen(context, HIGH), "arrival gate open");
          context.assertFalse(isOpen(context, MIDDLE), "passed gate shut");
          context.assertTrue(
              MathHelper.approximatelyEquals(
                  MathHelper.wrapDegrees(rider.player().getYaw()),
                  MathHelper.wrapDegrees(Direction.WEST.asRotation())),
              "faces out, yaw " + rider.player().getYaw());
          context.assertTrue(rider.hasEarned(GOING_UP), "Going Up earned");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void theRiderStaysInTheCarWholeRide(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtEveryTick(
        () -> {
          long tick = context.getTick();
          if (tick > START_TICK + 2 && tick < START_TICK + TICKS_FOR_SIX_BLOCKS - 2) {
            ServerPlayerEntity player = rider.player();
            player.setSneaking(true);
            LiftCarEntity car = car(context);
            context.assertTrue(player.hasVehicle(), "dropped out at tick " + tick);
            context.assertFalse(player.isInsideWall(), "in a wall at tick " + tick);
            context.assertTrue(
                Math.abs(player.getY() - (car.getY() + LiftCarEntity.PLATE)) < 0.51,
                "rider is standing on the car");
          }
        });
    context.runAtTick(START_TICK + SIX_BLOCK_RIDE_TICKS, () -> finish(context, rider));
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aCellPaysPerBlockMovedAndOnlyWhileMoving(TestContext context) {
    RedstoneCellBlockEntity cell = build(context, FULL_CHARGE);
    int perBlock = Hardwired.SERVER_CONFIG.lift().chargePerBlock();
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, MIDDLE);
        });
    context.runAtTick(
        START_TICK + 8,
        () ->
            context.assertTrue(
                cell.charge() > FULL_CHARGE - 3 * perBlock,
                "paid for the whole trip up front: " + cell.charge()));
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () ->
            context.assertEquals(FULL_CHARGE - 3 * perBlock, cell.charge(), "three blocks moved"));
    context.runAtTick(
        START_TICK + 2 * SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertEquals(FULL_CHARGE - 3 * perBlock, cell.charge(), "a waiting lift is free");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aClosedGateOpensForNothingButTheCar(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    ConnectedTestPlayer standing = ConnectedTestPlayer.joinAt(context, gate(MIDDLE).west(0));
    context.runAtTick(START_TICK, () -> openAt(context, rider, LOW));
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.setBlockState(gate(MIDDLE).north(), Blocks.REDSTONE_BLOCK);
          context.setBlockState(gate(MIDDLE).up().north(), Blocks.REDSTONE_BLOCK);
        });
    context.runAtTick(
        START_TICK + 6,
        () -> {
          context.assertFalse(isOpen(context, MIDDLE), "redstone opened a gate");
          BlockPos closed = context.getAbsolutePos(gate(MIDDLE));
          context.assertFalse(
              context
                  .getWorld()
                  .getBlockState(closed)
                  .getCollisionShape(context.getWorld(), closed)
                  .isEmpty(),
              "closed gate has collision");
          useGate(context, standing, MIDDLE);
          context.assertFalse(isOpen(context, MIDDLE), "a hand opened a gate");
          context.assertTrue(isOpen(context, LOW), "the car floor stays open");
        });
    context.runAtTick(
        START_TICK + 40,
        () -> {
          context.assertTrue(isOpen(context, MIDDLE), "the called car opens the gate");
          context.assertFalse(isOpen(context, LOW), "the left floor shut");
          finish(context, rider, standing);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void callsFromOtherFloorsQueueWhileRiding(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    ConnectedTestPlayer caller = ConnectedTestPlayer.joinAt(context, gate(LOW));
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, MIDDLE);
          choose(context, rider, MIDDLE, HIGH);
        });
    context.runAtTick(START_TICK + 4, () -> useGate(context, caller, LOW));
    context.runAtTick(
        START_TICK + 34,
        () -> {
          expectAtGate(context, rider, HIGH, "rider");
          context.assertTrue(
              car(context).run().restingAt(context.getAbsolutePos(gate(HIGH)).getY()),
              "car waits at the top");
        });
    context.runAtTick(
        START_TICK + 34 + 90,
        () -> {
          context.assertTrue(isOpen(context, LOW), "the queued call is answered");
          context.assertFalse(isOpen(context, HIGH), "the top gate shut behind it");
          finish(context, rider, caller);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void twoRidersBothArriveSafely(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer first = riderFrom(context, LOW);
    ConnectedTestPlayer second = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, first, LOW);
          choose(context, first, LOW, MIDDLE);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          context.assertEquals(2, car(context).getPassengerList().size(), "both aboard");
          context.assertTrue(
              first.player().squaredDistanceTo(second.player()) > 0.01, "riders stand apart");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectAtGate(context, first, MIDDLE, "first");
          expectAtGate(context, second, MIDDLE, "second");
          context.assertTrue(first.hasEarned(GOING_UP), "first earned it");
          context.assertTrue(second.hasEarned(GOING_UP), "second earned it");
          finish(context, first, second);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void runningDryStopsThenLowersToTheFloorBelow(TestContext context) {
    int perBlock = Hardwired.SERVER_CONFIG.lift().chargePerBlock();
    RedstoneCellBlockEntity cell = build(context, 4 * perBlock);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 36,
        () -> {
          LiftCarEntity car = car(context);
          context.assertTrue(
              car.run().phase() == LiftPhase.STOPPED, "stopped: " + car.run().phase());
          context.assertTrue(
              car.run().stopReason() == DeviceStatus.NO_CHARGE, "reason " + car.run().stopReason());
          context.assertEquals(0, cell.charge(), "every block paid for");
          context.assertTrue(rider.player().hasVehicle(), "still riding while stopped");
          ElectronicBlockEntity head = context.getBlockEntity(HEAD);
          context.assertTrue(head.status() == DeviceStatus.NO_CHARGE, "head shows why");
        });
    context.runAtTick(
        START_TICK + 150,
        () -> {
          expectAtGate(context, rider, MIDDLE, "rider");
          context.assertTrue(isOpen(context, MIDDLE), "gate opened at the floor below");
          context.assertEquals(0, cell.charge(), "lowering is free");
          context.assertFalse(rider.hasEarned(GOING_UP), "a failed ride earns nothing");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void chargingAgainAnswersTheNextCall(TestContext context) {
    int perBlock = Hardwired.SERVER_CONFIG.lift().chargePerBlock();
    RedstoneCellBlockEntity cell = build(context, perBlock);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 140,
        () -> {
          expectAtGate(context, rider, LOW, "rider");
          cell.setCharge(FULL_CHARGE);
          rider
              .player()
              .requestTeleport(
                  context.getAbsolute(net.minecraft.util.math.Vec3d.ofBottomCenter(inCar(LOW))).x,
                  rider.player().getY(),
                  context.getAbsolute(net.minecraft.util.math.Vec3d.ofBottomCenter(inCar(LOW))).z);
          choose(context, rider, LOW, MIDDLE);
        });
    context.runAtTick(
        START_TICK + 140 + SIX_BLOCK_RIDE_TICKS,
        () -> {
          expectAtGate(context, rider, MIDDLE, "rider");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void withNoFloorBelowRidersGetSlowFalling(TestContext context) {
    int perBlock = Hardwired.SERVER_CONFIG.lift().chargePerBlock();
    build(context, 2 * perBlock);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, MIDDLE);
          choose(context, rider, MIDDLE, HIGH);
        });
    context.runAtTick(
        START_TICK + 4,
        () -> {
          context.setBlockState(gate(LOW).up(), Blocks.AIR);
          context.setBlockState(gate(LOW), Blocks.AIR);
          context.setBlockState(gate(MIDDLE).up(), Blocks.AIR);
          context.setBlockState(gate(MIDDLE), Blocks.AIR);
        });
    context.runAtTick(
        START_TICK + 80,
        () -> {
          context.assertFalse(rider.player().hasVehicle(), "rider let down");
          context.expectEntityHasEffect(rider.player(), StatusEffects.SLOW_FALLING, 0);
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aBlockInTheShaftSendsTheCarBackToTheLastSafeFloor(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(START_TICK + 16, () -> context.setBlockState(inCar(HIGH), Blocks.STONE));
    context.runAtTick(
        START_TICK + 80,
        () -> {
          expectAtGate(context, rider, MIDDLE, "rider");
          context.assertTrue(isOpen(context, MIDDLE), "safe floor open");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aBlockedShaftRefusesTheRideBeforeItStarts(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          context.setBlockState(inCar(MIDDLE), Blocks.STONE);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 10,
        () -> {
          context.assertFalse(rider.player().hasVehicle(), "no ride into a blocked shaft");
          context.assertTrue(isOpen(context, LOW), "gate stays open");
          ElectronicBlockEntity gate = context.getBlockEntity(gate(LOW));
          context.assertTrue(gate.status() == DeviceStatus.SHAFT_BLOCKED, "says why");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void breakingTheLiftMidRideLeavesRidersSafeAndDropsOneItem(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 22, () -> context.getWorld().breakBlock(context.getAbsolutePos(HEAD), true));
    context.runAtTick(
        START_TICK + 26,
        () -> {
          context.assertFalse(rider.player().hasVehicle(), "rider off the car");
          context.assertTrue(shaft(context).car().isEmpty(), "car removed");
          BlockPos spot = rider.player().getBlockPos();
          context.assertTrue(
              spot.equals(context.getAbsolutePos(gate(MIDDLE))), "at the nearest gate: " + spot);
          context.assertFalse(isOpen(context, MIDDLE), "no car, no open gate");
          Box box = context.getTestBox();
          int dropped =
              context.getWorld().getEntitiesByClass(ItemEntity.class, box, item -> true).stream()
                  .filter(item -> item.getStack().isOf(ModBlocks.LIFT.asItem()))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "Lift items dropped");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aRiderWhoLeavesIsPlacedSafelyAndTheCarCarriesOn(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer leaver = ConnectedTestPlayer.joinAt(context, inCar(LOW));
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, leaver, LOW);
          choose(context, leaver, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 22,
        () -> {
          ServerPlayerEntity player = leaver.player();
          leaver.leave();
          context.assertTrue(
              player.getBlockPos().equals(context.getAbsolutePos(gate(MIDDLE))),
              "saved at the nearest gate: " + player.getBlockPos());
          context.assertTrue(shaft(context).car().isPresent(), "the car survives its rider");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          context.assertTrue(isOpen(context, HIGH), "the car finished its trip");
          finish(context);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aSavedCarResumesItsTripAfterAChunkReload(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          choose(context, rider, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 12,
        () -> {
          LiftCarEntity saved = car(context);
          saved.removeAllPassengers();
          NbtCompound nbt = new NbtCompound();
          saved.writeNbt(nbt);
          saved.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
          LiftCarEntity loaded = ModEntities.LIFT_CAR.create(context.getWorld());
          loaded.readNbt(nbt);
          loaded.setUuid(UUID.randomUUID());
          context.getWorld().spawnEntity(loaded);
          context.assertTrue(loaded.run().phase() == LiftPhase.MOVING, "still moving");
        });
    context.runAtTick(
        START_TICK + SIX_BLOCK_RIDE_TICKS,
        () -> {
          LiftCarEntity car = car(context);
          context.assertTrue(
              car.run().restingAt(context.getAbsolutePos(gate(HIGH)).getY()), "resumed to the top");
          context.assertTrue(isOpen(context, HIGH), "gate opened on arrival");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aChoiceFromOutOfReachOrASpectatorIsIgnored(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, LOW);
    ConnectedTestPlayer far = ConnectedTestPlayer.joinAt(context, new BlockPos(5, HIGH + 4, 5));
    ConnectedTestPlayer watcher = riderFrom(context, LOW);
    context.runAtTick(
        START_TICK,
        () -> {
          openAt(context, rider, LOW);
          watcher.player().changeGameMode(GameMode.SPECTATOR);
          choose(context, far, LOW, HIGH);
          choose(context, watcher, LOW, HIGH);
        });
    context.runAtTick(
        START_TICK + 10,
        () -> {
          context.assertTrue(car(context).run().phase() == LiftPhase.RESTING, "no one rode");
          context.assertFalse(rider.player().hasVehicle(), "no one mounted");
          finish(context, rider, far, watcher);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void floorsComeBackOnlyForThisShaftInHeightOrder(TestContext context) {
    build(context, FULL_CHARGE);
    context.setBlockState(new BlockPos(COLUMN_X + 1, LOW, COLUMN_Z), Blocks.STONE);
    int base = context.getAbsolutePos(BlockPos.ORIGIN).getY();
    List<Integer> levels = LiftShaft.levels(shaft(context).gates());
    context.assertTrue(
        levels.equals(List.of(base + LOW, base + MIDDLE, base + HIGH)), "levels " + levels);
    context.complete();
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void aCarIsGroundForAPlayerStandingOnIt(TestContext context) {
    build(context, FULL_CHARGE);
    ConnectedTestPlayer rider = riderFrom(context, MIDDLE);
    context.runAtTick(START_TICK, () -> openAt(context, rider, MIDDLE));
    context.runAtTick(
        START_TICK + 2,
        () -> {
          context.assertTrue(LiftCarEntity.holdsUp(rider.player()), "stands on the plate");
          finish(context, rider);
        });
  }

  @GameTest(templateName = SHAFT, tickLimit = RIDE_TICK_LIMIT)
  public void breakingAGateDropsOneItem(TestContext context) {
    build(context, FULL_CHARGE);
    context.getWorld().breakBlock(context.getAbsolutePos(gate(MIDDLE).up()), true);
    context.runAtTick(
        2,
        () -> {
          context.expectBlock(Blocks.AIR, gate(MIDDLE));
          ServerWorld world = context.getWorld();
          Item gateItem = ModBlocks.LIFT_FLOOR.asItem();
          int dropped =
              world
                  .getEntitiesByClass(ItemEntity.class, context.getTestBox(), item -> true)
                  .stream()
                  .filter(item -> item.getStack().isOf(gateItem))
                  .mapToInt(item -> item.getStack().getCount())
                  .sum();
          context.assertEquals(dropped, 1, "gate items dropped");
          context.complete();
        });
  }
}
