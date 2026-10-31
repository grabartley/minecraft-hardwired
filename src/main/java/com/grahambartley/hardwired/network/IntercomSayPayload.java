package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.IntercomLine;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record IntercomSayPayload(BlockPos pos, String line) implements CustomPayload {

  public static final Id<IntercomSayPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "intercom_say"));
  public static final PacketCodec<RegistryByteBuf, IntercomSayPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          IntercomSayPayload::pos,
          PacketCodecs.string(IntercomLine.MAX_LENGTH),
          IntercomSayPayload::line,
          IntercomSayPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
