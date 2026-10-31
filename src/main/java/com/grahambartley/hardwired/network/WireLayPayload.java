package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireLayPayload(List<WireFace> faces) implements CustomPayload {

  public static final int MAX_FACES = 64;
  public static final Id<WireLayPayload> ID = new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_lay"));
  public static final PacketCodec<RegistryByteBuf, WireLayPayload> CODEC =
      WireFace.PACKET_CODEC
          .collect(PacketCodecs.toList(MAX_FACES))
          .xmap(WireLayPayload::new, WireLayPayload::faces)
          .cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
