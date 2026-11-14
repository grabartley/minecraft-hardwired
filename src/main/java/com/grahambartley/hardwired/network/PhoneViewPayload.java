package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.phone.PartyLines;
import com.grahambartley.hardwired.phone.PhoneDirectory.Listing;
import com.grahambartley.hardwired.phone.PhoneEntry;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record PhoneViewPayload(
    BlockPos phone, int number, int status, List<Listing> listings, boolean open)
    implements CustomPayload {

  public static final int MAX_LISTINGS =
      PhoneNumbering.LAST_DIGIT * PhoneNumbering.LINES_PER_EXCHANGE * PartyLines.MOST_PHONES;

  private static final PacketCodec<RegistryByteBuf, Listing> LISTING =
      PacketCodec.tuple(
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          Listing::number,
          PacketCodecs.string(PhoneEntry.MAX_NAME).<RegistryByteBuf>cast(),
          Listing::name,
          Listing::new);

  public static final Id<PhoneViewPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "phone_view"));
  public static final PacketCodec<RegistryByteBuf, PhoneViewPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          PhoneViewPayload::phone,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          PhoneViewPayload::number,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          PhoneViewPayload::status,
          LISTING.collect(PacketCodecs.toList(MAX_LISTINGS)),
          PhoneViewPayload::listings,
          PacketCodecs.BOOL.<RegistryByteBuf>cast(),
          PhoneViewPayload::open,
          PhoneViewPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
