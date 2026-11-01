package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public record WireChunkPayload(long chunk, List<CellView> cells, List<MarkView> marks)
    implements CustomPayload {

  public static final int ON = 1;
  public static final int TRIPPED = 2;
  public static final int LOOSE_END = 4;
  public static final int SEALED = 8;
  public static final int POWERED = 16;

  public record CellView(long pos, int side, int color, int flags, int arms) {

    public static int armMask(Set<Direction> arms) {
      int mask = 0;
      for (Direction arm : arms) {
        mask |= 1 << arm.getId();
      }
      return mask;
    }

    public boolean hasArm(Direction direction) {
      return (arms & 1 << direction.getId()) != 0;
    }

    public boolean isSealed() {
      return (flags & SEALED) != 0;
    }

    public static final PacketCodec<RegistryByteBuf, CellView> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_LONG,
            CellView::pos,
            PacketCodecs.VAR_INT,
            CellView::side,
            PacketCodecs.VAR_INT,
            CellView::color,
            PacketCodecs.VAR_INT,
            CellView::flags,
            PacketCodecs.VAR_INT,
            CellView::arms,
            CellView::new);
  }

  public record MarkView(long pos, boolean input) {
    public static final PacketCodec<RegistryByteBuf, MarkView> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_LONG,
            MarkView::pos,
            PacketCodecs.BOOL,
            MarkView::input,
            MarkView::new);
  }

  public static final Id<WireChunkPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "wire_chunk"));
  public static final PacketCodec<RegistryByteBuf, WireChunkPayload> CODEC =
      PacketCodec.tuple(
          PacketCodecs.VAR_LONG,
          WireChunkPayload::chunk,
          CellView.CODEC.collect(PacketCodecs.toList()),
          WireChunkPayload::cells,
          MarkView.CODEC.collect(PacketCodecs.toList()),
          WireChunkPayload::marks,
          WireChunkPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
