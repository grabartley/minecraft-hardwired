package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public final class WireViewRange {

  private static final int CHUNK_SHIFT = 4;
  private static final int LAST_BLOCK_IN_CHUNK = 15;

  private WireViewRange() {}

  public static int chunkRadius(int range) {
    return (range + LAST_BLOCK_IN_CHUNK) >> CHUNK_SHIFT;
  }

  public static boolean contains(BlockPos viewer, long pos, int range) {
    return viewer.getSquaredDistance(BlockPos.fromLong(pos)) <= (double) range * range;
  }

  public static WireChunkPayload filter(WireChunkPayload payload, BlockPos viewer, int range) {
    List<CellView> cells =
        payload.cells().stream().filter(cell -> contains(viewer, cell.pos(), range)).toList();
    List<MarkView> marks =
        payload.marks().stream().filter(mark -> contains(viewer, mark.pos(), range)).toList();
    return new WireChunkPayload(payload.chunk(), cells, marks);
  }
}
