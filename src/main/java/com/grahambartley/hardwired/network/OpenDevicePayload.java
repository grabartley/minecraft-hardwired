package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenDevicePayload(BlockPos pos) implements CustomPayload {

  public static final Id<OpenDevicePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "open_device"));
  public static final PacketCodec<RegistryByteBuf, OpenDevicePayload> CODEC =
      BlockPos.PACKET_CODEC.xmap(OpenDevicePayload::new, OpenDevicePayload::pos).cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
