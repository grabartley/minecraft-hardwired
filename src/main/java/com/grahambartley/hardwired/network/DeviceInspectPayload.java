package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DeviceInspectPayload(BlockPos pos) implements CustomPayload {

  public static final Id<DeviceInspectPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "device_inspect"));
  public static final PacketCodec<RegistryByteBuf, DeviceInspectPayload> CODEC =
      PacketCodec.tuple(BlockPos.PACKET_CODEC, DeviceInspectPayload::pos, DeviceInspectPayload::new)
          .cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
