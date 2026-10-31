package com.grahambartley.hardwired.wire;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record WireFace(BlockPos pos, Direction side) {

  public static final PacketCodec<ByteBuf, WireFace> PACKET_CODEC =
      PacketCodec.tuple(
          BlockPos.PACKET_CODEC,
          WireFace::pos,
          Direction.PACKET_CODEC,
          WireFace::side,
          WireFace::new);

  public WireFace {
    pos = pos.toImmutable();
  }

  public BlockPos occupant() {
    return pos.offset(side);
  }
}
