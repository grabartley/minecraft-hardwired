package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DeviceInspectResultPayload(BlockPos pos, int load, int supply, boolean overloaded)
    implements CustomPayload {

  public static final Id<DeviceInspectResultPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "device_inspect_result"));
  public static final PacketCodec<RegistryByteBuf, DeviceInspectResultPayload> CODEC =
      PacketCodec.tuple(
              BlockPos.PACKET_CODEC,
              DeviceInspectResultPayload::pos,
              PacketCodecs.VAR_INT,
              DeviceInspectResultPayload::load,
              PacketCodecs.VAR_INT,
              DeviceInspectResultPayload::supply,
              PacketCodecs.BOOL,
              DeviceInspectResultPayload::overloaded,
              DeviceInspectResultPayload::new)
          .cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
