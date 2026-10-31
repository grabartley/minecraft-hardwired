package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireViewClearPayload() implements CustomPayload {

  public static final WireViewClearPayload INSTANCE = new WireViewClearPayload();
  public static final Id<WireViewClearPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_view_clear"));
  public static final PacketCodec<RegistryByteBuf, WireViewClearPayload> CODEC =
      PacketCodec.unit(INSTANCE);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
