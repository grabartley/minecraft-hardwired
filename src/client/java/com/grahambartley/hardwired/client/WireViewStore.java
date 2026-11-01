package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WireTopology;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireViewStore {

  private static final Long2ObjectOpenHashMap<WireChunkPayload> CHUNKS =
      new Long2ObjectOpenHashMap<>();

  private WireViewStore() {}

  public static void put(WireChunkPayload payload) {
    if (payload.cells().isEmpty()) {
      CHUNKS.remove(payload.chunk());
    } else {
      CHUNKS.put(payload.chunk(), payload);
    }
  }

  public static void clear() {
    CHUNKS.clear();
  }

  public static Collection<WireChunkPayload> chunks() {
    return CHUNKS.values();
  }

  public static Optional<CellView> cellAt(WireFace face) {
    return cellsOn(face).stream().findFirst();
  }

  public static List<CellView> cellsOn(WireFace face) {
    long pos = face.pos().asLong();
    return CHUNKS.values().stream()
        .flatMap(chunk -> chunk.cells().stream())
        .filter(cell -> cell.pos() == pos && cell.side() == face.side().getId())
        .toList();
  }

  public static Optional<WireFace> wiredFaceAt(BlockPos hit, Direction side) {
    return WireTopology.targets(hit, side).stream()
        .filter(face -> cellAt(face).isPresent())
        .findFirst();
  }

  public static boolean hasSealedCellAt(BlockPos hit, Direction side) {
    return WireTopology.targets(hit, side).stream()
        .flatMap(face -> cellsOn(face).stream())
        .anyMatch(CellView::isSealed);
  }
}
