package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ExchangeViewPayload(BlockPos exchange, int digit, List<Line> lines)
    implements CustomPayload {

  public record Line(BlockPos phone, int number, String name) {

    static final PacketCodec<RegistryByteBuf, Line> CODEC =
        PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            Line::phone,
            PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
            Line::number,
            PacketCodecs.string(PhoneViewPayload.MAX_NAME).<RegistryByteBuf>cast(),
            Line::name,
            Line::new);
  }

  public static final Id<ExchangeViewPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "exchange_view"));
  public static final PacketCodec<RegistryByteBuf, ExchangeViewPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          ExchangeViewPayload::exchange,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          ExchangeViewPayload::digit,
          Line.CODEC.collect(PacketCodecs.toList(PhoneNumbering.LINES_PER_EXCHANGE)),
          ExchangeViewPayload::lines,
          ExchangeViewPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
