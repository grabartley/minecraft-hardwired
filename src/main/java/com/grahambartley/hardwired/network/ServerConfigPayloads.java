package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.config.WireSettings;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ServerConfigPayloads {

  private static final PacketCodec<RegistryByteBuf, HardwiredConfig> CONFIG_CODEC =
      PacketCodec.of(ServerConfigPayloads::write, ServerConfigPayloads::read);

  private ServerConfigPayloads() {}

  private static void write(HardwiredConfig config, RegistryByteBuf buf) {
    buf.writeVarInt(config.wire().viewRangeBlocks());
    buf.writeVarInt(config.wire().maxNetworkSize());
    buf.writeVarInt(config.lamp().ageRateMultiplierPercent());
    buf.writeVarInt(config.device().motionSensorRangeBlocks());
    buf.writeVarInt(config.device().buttonHoldTicks());
    buf.writeVarInt(config.device().intercomRangeBlocks());
    PowerSettings power = config.power();
    buf.writeVarInt(power.cellCapacity());
    buf.writeVarInt(power.generatorCapacity());
    buf.writeVarInt(power.dustCharge());
    buf.writeVarInt(power.lightningCharge());
    buf.writeVarInt(power.chargePerLoadMinute());
    buf.writeVarInt(power.cellRating());
    buf.writeVarInt(power.generatorRating());
    buf.writeVarInt(power.solarRating());
    buf.writeVarInt(power.dynamoRating());
    buf.writeVarInt(power.lampLoad());
    buf.writeVarInt(power.streetlightLoad());
    buf.writeVarInt(power.logicLoad());
    buf.writeVarInt(power.vanillaLoad());
    buf.writeVarInt(power.radioLoad());
    buf.writeVarInt(power.speakerLoad());
    buf.writeVarInt(power.intercomLoad());
    buf.writeVarInt(power.alarmLoad());
    buf.writeVarInt(power.broadcastLoad());
    buf.writeVarInt(power.liftLoad());
    buf.writeVarInt(power.humRange());
    buf.writeVarInt(power.humStartLoad());
    buf.writeVarInt(power.humFullLoad());
    buf.writeVarInt(config.lift().ticksPerBlock());
    buf.writeVarInt(config.circuit().maxParts());
    buf.writeVarInt(config.circuit().partsPerRedstone());
    RadioSettings radio = config.radio();
    buf.writeVarInt(radio.towerMinMasts());
    buf.writeVarInt(radio.towerMaxMasts());
    buf.writeVarInt(radio.towerBaseReach());
    buf.writeVarInt(radio.towerReachPerMast());
    buf.writeVarInt(radio.radioHearing());
    buf.writeVarInt(radio.speakerHearing());
    buf.writeVarInt(radio.undergroundFreeDepth());
    buf.writeVarInt(radio.undergroundLossDepth());
    buf.writeBoolean(radio.voiceBroadcasts());
  }

  private static HardwiredConfig read(RegistryByteBuf buf) {
    return new HardwiredConfig(
        new WireSettings(buf.readVarInt(), buf.readVarInt()),
        new LampSettings(buf.readVarInt()),
        new DeviceSettings(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()),
        new PowerSettings(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()),
        new LiftSettings(buf.readVarInt()),
        new CircuitSettings(buf.readVarInt(), buf.readVarInt()),
        new RadioSettings(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean()));
  }

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
