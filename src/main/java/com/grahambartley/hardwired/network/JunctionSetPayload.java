package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record JunctionSetPayload(BlockPos pos, int rule, int output) implements CustomPayload {

  public static final Id<JunctionSetPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "junction_set"));
  public static final PacketCodec<RegistryByteBuf, JunctionSetPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          JunctionSetPayload::pos,
          PacketCodecs.VAR_INT,
          JunctionSetPayload::rule,
          PacketCodecs.VAR_INT,
          JunctionSetPayload::output,
          JunctionSetPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
