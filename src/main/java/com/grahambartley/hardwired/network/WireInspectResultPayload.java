package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireInspectResultPayload(
    int color,
    boolean on,
    boolean powered,
    boolean tripped,
    int load,
    int supply,
    List<String> drivers)
    implements CustomPayload {

  public static final int MAX_DRIVERS = 6;
  public static final Id<WireInspectResultPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_inspect_result"));
  private static final PacketCodec<ByteBuf, List<String>> DRIVERS =
      PacketCodecs.STRING.collect(PacketCodecs.toList(MAX_DRIVERS));
  public static final PacketCodec<RegistryByteBuf, WireInspectResultPayload> CODEC =
      PacketCodec.of(WireInspectResultPayload::write, WireInspectResultPayload::read);

  private void write(RegistryByteBuf buf) {
    buf.writeVarInt(color);
    buf.writeBoolean(on);
    buf.writeBoolean(powered);
    buf.writeBoolean(tripped);
    buf.writeVarInt(load);
    buf.writeVarInt(supply);
    DRIVERS.encode(buf, drivers);
  }

  private static WireInspectResultPayload read(RegistryByteBuf buf) {
    return new WireInspectResultPayload(
        buf.readVarInt(),
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readVarInt(),
        buf.readVarInt(),
        DRIVERS.decode(buf));
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
