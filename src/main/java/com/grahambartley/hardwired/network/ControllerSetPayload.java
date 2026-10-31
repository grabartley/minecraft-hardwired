package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.controller.ControllerProgram;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ControllerSetPayload(BlockPos pos, ControllerProgram program)
    implements CustomPayload {

  public static final Id<ControllerSetPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "controller_set"));
  public static final PacketCodec<RegistryByteBuf, ControllerSetPayload> CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          ControllerSetPayload::pos,
          ControllerProgram.PACKET_CODEC,
          ControllerSetPayload::program,
          ControllerSetPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
