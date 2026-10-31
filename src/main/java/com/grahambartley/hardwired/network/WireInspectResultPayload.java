package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireInspectResultPayload(int color, boolean on, boolean tripped, List<String> drivers)
    implements CustomPayload {

  public static final int MAX_DRIVERS = 6;
  public static final Id<WireInspectResultPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_inspect_result"));
  public static final PacketCodec<RegistryByteBuf, WireInspectResultPayload> CODEC =
      PacketCodec.tuple(
              PacketCodecs.VAR_INT,
              WireInspectResultPayload::color,
              PacketCodecs.BOOL,
              WireInspectResultPayload::on,
              PacketCodecs.BOOL,
              WireInspectResultPayload::tripped,
              PacketCodecs.STRING.collect(PacketCodecs.toList(MAX_DRIVERS)),
              WireInspectResultPayload::drivers,
              WireInspectResultPayload::new)
          .cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
