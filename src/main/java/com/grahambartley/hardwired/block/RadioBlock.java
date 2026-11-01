package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.radio.RadioTuner;
import com.grahambartley.hardwired.radio.Reception;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class RadioBlock extends TableDeviceBlock {

  public static final BooleanProperty PLAYING = BooleanProperty.of("playing");

  public RadioBlock(Settings settings, double[] box) {
    super(settings, box);
  }

  @Override
  protected BooleanProperty live() {
    return PLAYING;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new RadioBlockEntity(pos, state);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world, BlockState state, BlockEntityType<T> type) {
    if (world.isClient || type != ModBlockEntities.RADIO) {
      return null;
    }
    return (BlockEntityTicker<T>)
        (BlockEntityTicker<RadioBlockEntity>) RadioBlockEntity::spillOldDisc;
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return DeviceActions.open(player, pos);
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())
        && world instanceof ServerWorld server
        && world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      radio.setStatus(DeviceStatus.RADIO_OFF);
      wakeSpeakers(server, pos);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    if (!(world.getBlockEntity(pos) instanceof RadioBlockEntity radio) || !radio.isOn()) {
      return DeviceStatus.RADIO_OFF;
    }
    return RadioTuner.best(world, pos, radio.frequency()).isPresent()
        ? DeviceStatus.RUNNING
        : DeviceStatus.NO_STATION;
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.radioLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return world.getBlockEntity(pos) instanceof RadioBlockEntity radio && radio.isOn()
        ? ratedLoad(Hardwired.SERVER_CONFIG.power())
        : 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    boolean playing = status == DeviceStatus.RUNNING;
    if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      Optional<Reception> heard =
          playing ? RadioTuner.best(world, pos, radio.frequency()) : Optional.empty();
      heard.ifPresentOrElse(
          reception ->
              radio.hear(
                  reception.station().desk(),
                  reception.station().song(),
                  reception.station().songStart(),
                  reception.strength()),
          radio::silence);
      radio.setHissing(
          status == DeviceStatus.NO_STATION && WireEngine.of(world).power().hasPower(pos));
    }
    if (state.get(PLAYING) != playing) {
      ElectronicsDriver.show(world, pos, state, state.with(PLAYING, playing));
      wakeSpeakers(world, pos);
    }
  }

  @Override
  public void beat(ServerWorld world, BlockPos pos) {
    if (world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      radio.noteParticle(world);
    }
  }

  @Override
  public boolean keepsChecking(DeviceStatus status) {
    return status != DeviceStatus.RADIO_OFF;
  }

  private static void wakeSpeakers(ServerWorld world, BlockPos pos) {
    WireEngine.of(world)
        .allOnNetwork(pos, SpeakerBlockEntity.class)
        .forEach(speaker -> ElectronicsDriver.wake(world, speaker.getPos()));
  }
}
