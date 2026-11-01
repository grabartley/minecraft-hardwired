package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.network.WireChunkPayload;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.LongToIntFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class WireViewTracker {

  private final Long2IntOpenHashMap sent = new Long2IntOpenHashMap();
  private final Long2ObjectOpenHashMap<WireChunkPayload> described = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectOpenHashMap<WireChunkPayload> shown = new Long2ObjectOpenHashMap<>();
  private BlockPos origin;
  private int range;

  public List<WireChunkPayload> update(
      BlockPos viewer,
      int viewRange,
      LongToIntFunction versions,
      LongFunction<WireChunkPayload> describe) {
    boolean moved = !viewer.equals(origin) || viewRange != range;
    origin = viewer.toImmutable();
    range = viewRange;
    int radius = WireViewRange.chunkRadius(range);
    ChunkPos center = new ChunkPos(origin);
    List<WireChunkPayload> updates = new ArrayList<>();
    for (int x = center.x - radius; x <= center.x + radius; x++) {
      for (int z = center.z - radius; z <= center.z + radius; z++) {
        long chunk = ChunkPos.toLong(x, z);
        boolean changed = refresh(chunk, versions, describe);
        if (changed || moved) {
          showIfDifferent(chunk, updates);
        }
      }
    }
    forgetChunksOutOfRange(center, radius, updates);
    return updates;
  }

  private boolean refresh(
      long chunk, LongToIntFunction versions, LongFunction<WireChunkPayload> describe) {
    int version = versions.applyAsInt(chunk);
    if (sent.get(chunk) == version) {
      return false;
    }
    sent.put(chunk, version);
    described.put(chunk, describe.apply(chunk));
    return true;
  }

  private void showIfDifferent(long chunk, List<WireChunkPayload> updates) {
    WireChunkPayload full = described.get(chunk);
    if (full == null) {
      return;
    }
    WireChunkPayload visible = WireViewRange.filter(full, origin, range);
    WireChunkPayload last = shown.get(chunk);
    boolean empty = visible.cells().isEmpty();
    if (empty ? last == null : visible.equals(last)) {
      return;
    }
    if (empty) {
      shown.remove(chunk);
    } else {
      shown.put(chunk, visible);
    }
    updates.add(visible);
  }

  private void forgetChunksOutOfRange(ChunkPos center, int radius, List<WireChunkPayload> updates) {
    for (long chunk : sent.keySet().toLongArray()) {
      ChunkPos pos = new ChunkPos(chunk);
      if (Math.abs(pos.x - center.x) > radius || Math.abs(pos.z - center.z) > radius) {
        sent.remove(chunk);
        described.remove(chunk);
        if (shown.remove(chunk) != null) {
          updates.add(new WireChunkPayload(chunk, List.of(), List.of()));
        }
      }
    }
  }
}
