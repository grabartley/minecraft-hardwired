package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record VoiceFramePayload(BlockPos desk, byte[] adpcm) implements CustomPayload {

  private static final int READABLE_BYTES = Short.MAX_VALUE;

  public static final Id<VoiceFramePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "voice_frame"));
  public static final PacketCodec<RegistryByteBuf, VoiceFramePayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          VoiceFramePayload::desk,
          PacketCodecs.byteArray(READABLE_BYTES),
          VoiceFramePayload::adpcm,
          VoiceFramePayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
