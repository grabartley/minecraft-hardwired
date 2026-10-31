package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.circuit.DesignText;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CircuitResultPayload(BlockPos pos, String design, String message, int amount)
    implements CustomPayload {

  private static final int MAX_MESSAGE_LENGTH = 128;

  public static final Id<CircuitResultPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "circuit_result"));
  public static final PacketCodec<RegistryByteBuf, CircuitResultPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          CircuitResultPayload::pos,
          PacketCodecs.string(DesignText.MAX_LENGTH),
          CircuitResultPayload::design,
          PacketCodecs.string(MAX_MESSAGE_LENGTH),
          CircuitResultPayload::message,
          PacketCodecs.VAR_INT,
          CircuitResultPayload::amount,
          CircuitResultPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
