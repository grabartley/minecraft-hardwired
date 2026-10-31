package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.device.CircuitAction;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CircuitActionPayload(BlockPos pos, int action, String text) implements CustomPayload {

  public static final Id<CircuitActionPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "circuit_action"));
  public static final PacketCodec<RegistryByteBuf, CircuitActionPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          CircuitActionPayload::pos,
          PacketCodecs.VAR_INT,
          CircuitActionPayload::action,
          PacketCodecs.string(DesignText.MAX_LENGTH),
          CircuitActionPayload::text,
          CircuitActionPayload::new);

  public static CircuitActionPayload of(BlockPos pos, CircuitAction action, String text) {
    return new CircuitActionPayload(pos, action.ordinal(), text);
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
