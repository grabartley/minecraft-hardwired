package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;

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
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).allOnNetwork(pos, RadioBlockEntity.class).stream()
            .anyMatch(radio -> radio.status().wantsPower())
        ? DeviceStatus.RUNNING
        : DeviceStatus.NO_RADIO;
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().speakerLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).allOnNetwork(pos, RadioBlockEntity.class).stream()
            .anyMatch(RadioBlockEntity::isOn)
        ? ratedLoad(Hardwired.SERVER_CONFIG.power())
        : 0;
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
      Optional<RadioBlockEntity> radio =
          status == DeviceStatus.RUNNING ? receivingRadio(world, pos) : Optional.empty();
      radio.ifPresentOrElse(speaker::repeat, speaker::silence);
    }
    ElectronicsDriver.show(world, pos, state, state.with(PLAYING, status == DeviceStatus.RUNNING));
  }

  @Override
  public void beat(ServerWorld world, BlockPos pos) {
    if (world.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
      speaker.noteParticle(world);
    }
  }

  private static Optional<RadioBlockEntity> receivingRadio(ServerWorld world, BlockPos pos) {
    return WireEngine.of(world).allOnNetwork(pos, RadioBlockEntity.class).stream()
        .filter(radio -> radio.status() == DeviceStatus.RUNNING)
        .max(Comparator.comparing(RadioBlockEntity::sounding));
  }
}
