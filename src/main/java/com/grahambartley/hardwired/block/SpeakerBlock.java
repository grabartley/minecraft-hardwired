package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SpeakerBlock extends TableDeviceBlock {

  public static final BooleanProperty PLAYING = BooleanProperty.of("playing");

  public SpeakerBlock(Settings settings, double[] box) {
    super(settings, box);
  }

  @Override
  protected BooleanProperty live() {
    return PLAYING;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new SpeakerBlockEntity(pos, state);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world, BlockState state, BlockEntityType<T> type) {
    if (world.isClient || !state.get(PLAYING) || type != ModBlockEntities.SPEAKER) {
      return null;
    }
    return (BlockEntityTicker<T>) (BlockEntityTicker<SpeakerBlockEntity>) SpeakerBlockEntity::tick;
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())
        && world.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
      speaker.silence();
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return playingRadio(world, pos).isPresent() ? DeviceStatus.RUNNING : DeviceStatus.NO_RADIO;
  }

  @Override
  public int chargePerSecond() {
    return Hardwired.SERVER_CONFIG.power().speakerChargePerSecond();
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
      if (status == DeviceStatus.RUNNING) {
        playingRadio(world, pos).flatMap(RadioBlockEntity::song).ifPresent(speaker::play);
      } else {
        speaker.silence();
      }
    }
    ElectronicsDriver.show(world, pos, state, state.with(PLAYING, status == DeviceStatus.RUNNING));
  }

  private static Optional<RadioBlockEntity> playingRadio(ServerWorld world, BlockPos pos) {
    return WireEngine.of(world).allOnNetwork(pos, RadioBlockEntity.class).stream()
        .filter(radio -> radio.status() == DeviceStatus.RUNNING)
        .findFirst();
  }
}
