package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.WireSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ServerConfigPayloads {

  private static final PacketCodec<RegistryByteBuf, HardwiredConfig> CONFIG_CODEC =
      PacketCodec.tuple(
          PacketCodecs.VAR_INT,
          config -> config.wire().viewRangeBlocks(),
          PacketCodecs.VAR_INT,
          config -> config.wire().maxNetworkSize(),
          PacketCodecs.VAR_INT,
          config -> config.lamp().ageRateMultiplierPercent(),
          PacketCodecs.VAR_INT,
          config -> config.device().motionSensorRangeBlocks(),
          PacketCodecs.VAR_INT,
          config -> config.device().buttonHoldTicks(),
          (viewRange, networkSize, ageRate, motionRange, buttonHold) ->
              new HardwiredConfig(
                  new WireSettings(viewRange, networkSize),
                  new LampSettings(ageRate),
                  new DeviceSettings(motionRange, buttonHold)));

  private ServerConfigPayloads() {}

  public record SyncServerConfigS2CPayload(HardwiredConfig config) implements CustomPayload {
    public static final CustomPayload.Id<SyncServerConfigS2CPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Hardwired.MOD_ID, "sync_server_config"));
    public static final PacketCodec<RegistryByteBuf, SyncServerConfigS2CPayload> CODEC =
        CONFIG_CODEC.xmap(SyncServerConfigS2CPayload::new, SyncServerConfigS2CPayload::config);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record EditServerConfigC2SPayload(HardwiredConfig config) implements CustomPayload {
    public static final CustomPayload.Id<EditServerConfigC2SPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Hardwired.MOD_ID, "edit_server_config"));
    public static final PacketCodec<RegistryByteBuf, EditServerConfigC2SPayload> CODEC =
        CONFIG_CODEC.xmap(EditServerConfigC2SPayload::new, EditServerConfigC2SPayload::config);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
      return ID;
    }
  }
}
