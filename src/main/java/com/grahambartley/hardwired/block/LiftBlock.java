package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCharge;
import com.grahambartley.hardwired.lift.LiftPhase;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.registry.ModEntities;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class LiftBlock extends TableDeviceBlock {

  public static final BooleanProperty RUNNING = BooleanProperty.of("running");

  private static final double[] FULL_BOX = {0, 0, 0, 16, 16, 16};

  public LiftBlock(Settings settings) {
    super(settings, FULL_BOX);
  }

  public static int spanAt(World world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof LiftBlock lift ? lift.span() : 1;
  }

  public int span() {
    return 1;
  }

  public BlockPos head(BlockPos pos, BlockState state) {
    return pos;
  }

  public EntityType<LiftCarEntity> carType() {
    return ModEntities.LIFT_CAR;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    ElectronicsDriver.wake(world, head(pos, state));
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, head(pos, state));
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return super.onUse(state, world, head(pos, state), player, hit);
  }

  @Override
  protected BooleanProperty live() {
    return RUNNING;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    BlockPos head = head(pos, state);
    Optional<LiftCarEntity> car = new LiftShaft(world, head).car();
    PowerGrids power = WireEngine.of(world).power();
    return LiftCharge.demand(
        car.map(found -> found.run().phase()).orElse(LiftPhase.RESTING),
        car.map(found -> found.run().stopReason()).orElse(DeviceStatus.READY),
        power.hasPower(head) ? DeviceStatus.READY : power.stopReason(head));
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().liftLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    boolean moving =
        new LiftShaft(world, head(pos, state))
            .car()
            .filter(car -> car.run().phase() == LiftPhase.MOVING)
            .isPresent();
    return moving ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    LiftShaft shaft = new LiftShaft(world, head(pos, state));
    boolean running = shaft.car().filter(car -> car.run().phase().travelling()).isPresent();
    for (BlockPos cell : cells(pos, state)) {
      BlockState cellState = world.getBlockState(cell);
      if (cellState.isOf(this)) {
        ElectronicsDriver.show(world, cell, cellState, cellState.with(RUNNING, running));
      }
    }
    shaft.mirror(shaft.gates(), status);
  }
}
