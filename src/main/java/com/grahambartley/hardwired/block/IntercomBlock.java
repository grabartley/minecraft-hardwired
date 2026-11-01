package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.IntercomActions;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class IntercomBlock extends TableDeviceBlock {

  public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

  private static final int RING_TICKS = 30;
  private static final float RING_VOLUME = 0.8F;
  private static final float RING_PITCH = 1.6F;

  public IntercomBlock(Settings settings, double[] box) {
    super(settings, box);
  }

  @Override
  protected BooleanProperty live() {
    return ACTIVE;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      IntercomActions.open(serverPlayer, pos);
    }
    return ActionResult.SUCCESS;
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.show(world, pos, state, state.with(ACTIVE, false));
    ElectronicsDriver.pulse(world, pos);
  }

  public void ring(ServerWorld world, BlockPos pos, BlockState state) {
    ElectronicsDriver.show(world, pos, state, state.with(ACTIVE, true));
    world.playSound(
        null,
        pos,
        SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
        SoundCategory.BLOCKS,
        RING_VOLUME,
        RING_PITCH);
    world.scheduleBlockTick(pos, this, RING_TICKS);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return IntercomActions.readiness(world, pos);
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).isWired(pos) ? Hardwired.SERVER_CONFIG.power().intercomLoad() : 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {}
}
