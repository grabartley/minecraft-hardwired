package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RadioSetPayload(BlockPos pos, boolean on, int frequency, int volume)
    implements CustomPayload {

  public static final Id<RadioSetPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "radio_set"));
  public static final PacketCodec<RegistryByteBuf, RadioSetPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          RadioSetPayload::pos,
          PacketCodecs.BOOL,
          RadioSetPayload::on,
          PacketCodecs.VAR_INT,
          RadioSetPayload::frequency,
          PacketCodecs.VAR_INT,
          RadioSetPayload::volume,
          RadioSetPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
