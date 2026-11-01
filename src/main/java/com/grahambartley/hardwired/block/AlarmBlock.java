package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class AlarmBlock extends MountedDeviceBlock
    implements BlockEntityProvider, AnimatedDevice, Electronic {

  private static final float RING_VOLUME = 2F;
  private static final float RING_PITCH = 1F;

  public AlarmBlock(Settings settings, double[] floorBox) {
    super(settings, floorBox);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return state.get(POWERED);
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    ElectronicsDriver.wake(world, pos);
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, 1);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (!world.isClient) {
      ElectronicsDriver.tell(world, pos, player);
    }
    return ActionResult.SUCCESS;
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).isOn(pos) ? DeviceStatus.RUNNING : DeviceStatus.WIRE_OFF;
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().alarmLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).isOn(pos) ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    ElectronicsDriver.show(world, pos, state, state.with(POWERED, status == DeviceStatus.RUNNING));
  }

  @Override
  public void beat(ServerWorld world, BlockPos pos) {
    world.playSound(
        null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, RING_VOLUME, RING_PITCH);
  }
}
