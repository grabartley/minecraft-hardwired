package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record LiftChoosePayload(BlockPos gate, int level) implements CustomPayload {

  public static final Id<LiftChoosePayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "lift_choose"));
  public static final PacketCodec<RegistryByteBuf, LiftChoosePayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          LiftChoosePayload::gate,
          PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
          LiftChoosePayload::level,
          LiftChoosePayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
