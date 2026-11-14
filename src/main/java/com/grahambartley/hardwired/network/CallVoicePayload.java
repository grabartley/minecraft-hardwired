package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.radio.VoiceFormat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CallVoicePayload(BlockPos phone, byte[] adpcm) implements CustomPayload {

  public static final Id<CallVoicePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "call_voice"));
  public static final PacketCodec<RegistryByteBuf, CallVoicePayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          CallVoicePayload::phone,
          PacketCodecs.byteArray(VoiceFormat.MOST_FRAME_BYTES),
          CallVoicePayload::adpcm,
          CallVoicePayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
