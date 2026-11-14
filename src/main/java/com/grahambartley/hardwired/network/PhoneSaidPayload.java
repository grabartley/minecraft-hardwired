package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record PhoneSaidPayload(BlockPos phone, Text line) implements CustomPayload {

  public static final Id<PhoneSaidPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "phone_said"));
  public static final PacketCodec<RegistryByteBuf, PhoneSaidPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          PhoneSaidPayload::phone,
          TextCodecs.REGISTRY_PACKET_CODEC,
          PhoneSaidPayload::line,
          PhoneSaidPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
