package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.WireFace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WireRemovePayload(WireFace face) implements CustomPayload {

  public static final Id<WireRemovePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_remove"));
  public static final PacketCodec<RegistryByteBuf, WireRemovePayload> CODEC =
      WireFace.PACKET_CODEC.xmap(WireRemovePayload::new, WireRemovePayload::face).cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
