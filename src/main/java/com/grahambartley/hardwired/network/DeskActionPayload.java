package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record DeskActionPayload(BlockPos pos, int action, int value) implements CustomPayload {

  public static final Id<DeskActionPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "desk_action"));
  public static final PacketCodec<RegistryByteBuf, DeskActionPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          DeskActionPayload::pos,
          PacketCodecs.VAR_INT,
          DeskActionPayload::action,
          PacketCodecs.VAR_INT,
          DeskActionPayload::value,
          DeskActionPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
