package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HumLevelPayload(int load) implements CustomPayload {

  public static final Id<HumLevelPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "hum_level"));
  public static final PacketCodec<RegistryByteBuf, HumLevelPayload> CODEC =
      PacketCodec.tuple(PacketCodecs.VAR_INT, HumLevelPayload::load, HumLevelPayload::new).cast();

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
