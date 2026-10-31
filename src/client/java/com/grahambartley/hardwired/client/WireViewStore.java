package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.wire.WireFace;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
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
    long pos = face.pos().asLong();
    return CHUNKS.values().stream()
        .flatMap(chunk -> chunk.cells().stream())
        .filter(cell -> cell.pos() == pos && cell.side() == face.side().getId())
        .findFirst();
  }

  public static Optional<WireFace> wiredFaceAt(BlockPos hit, Direction side) {
    WireFace surface = new WireFace(hit, side);
    WireFace hidden = new WireFace(hit.offset(side.getOpposite()), side);
    return cellAt(surface).isPresent() ? Optional.of(surface) : cellAt(hidden).map(cell -> hidden);
  }
}
