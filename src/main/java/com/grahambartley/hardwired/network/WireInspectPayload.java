package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.WireFace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireInspectPayload(WireFace face, int color) implements CustomPayload {

  public static final Id<WireInspectPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_inspect"));
  public static final PacketCodec<RegistryByteBuf, WireInspectPayload> CODEC =
      PacketCodec.tuple(
              WireFace.PACKET_CODEC,
              WireInspectPayload::face,
              PacketCodecs.VAR_INT,
              WireInspectPayload::color,
              WireInspectPayload::new)
          .cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
