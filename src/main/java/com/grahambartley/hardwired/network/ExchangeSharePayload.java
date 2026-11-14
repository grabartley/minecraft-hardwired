package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.phone.PartyLines;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ExchangeSharePayload(BlockPos exchange, List<BlockPos> phones, boolean share)
    implements CustomPayload {

  public static final Id<ExchangeSharePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "exchange_share"));
  public static final PacketCodec<RegistryByteBuf, ExchangeSharePayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          ExchangeSharePayload::exchange,
          BlockPos.PACKET_CODEC.collect(PacketCodecs.toList(PartyLines.MOST_PHONES)),
          ExchangeSharePayload::phones,
          PacketCodecs.BOOL.<RegistryByteBuf>cast(),
          ExchangeSharePayload::share,
          ExchangeSharePayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
