package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WireViewTrackerTest {

  private static final int RANGE = 32;
  private static final BlockPos NEAR = new BlockPos(8, 64, 8);
  private static final BlockPos TALL = NEAR.up(40);
  private static final long CHUNK = new ChunkPos(NEAR).toLong();

  private final Map<Long, Integer> versions = new HashMap<>();
  private final Map<Long, WireChunkPayload> world = new HashMap<>();
  private final List<Long> described = new ArrayList<>();
  private final WireViewTracker tracker = new WireViewTracker();

  private void place(BlockPos... cells) {
    List<CellView> views = new ArrayList<>();
    for (BlockPos cell : cells) {
      views.add(new CellView(cell.asLong(), 1, 0, 0, 0));
    }
    long chunk = new ChunkPos(cells[0]).toLong();
    world.put(chunk, new WireChunkPayload(chunk, views, List.of()));
    versions.merge(chunk, 1, Integer::sum);
  }

  private List<WireChunkPayload> update(BlockPos viewer) {
    return tracker.update(
        viewer,
        RANGE,
        chunk -> versions.getOrDefault(chunk, 0),
        chunk -> {
          described.add(chunk);
          return world.getOrDefault(chunk, new WireChunkPayload(chunk, List.of(), List.of()));
        });
  }

  private static int cellCount(List<WireChunkPayload> updates) {
    return updates.stream().mapToInt(payload -> payload.cells().size()).sum();
  }

  @Test
  void wirePastTheRangeInASentChunkIsNotSent() {
    place(NEAR, TALL);
    assertEquals(1, cellCount(update(NEAR)));
  }

  @ParameterizedTest(name = "moved up {0}: {1} cells")
  @CsvSource({"20, 2", "39, 1"})
  void movingWithinAChunkUpdatesWhatShows(int rise, int cells) {
    place(NEAR, TALL);
    update(NEAR);
    assertEquals(cells, cellCount(update(NEAR.up(rise))));
  }

  @Test
  void movingWithoutChangingWhatShowsSendsNothingAndDescribesNothing() {
    place(NEAR);
    update(NEAR);
    described.clear();
    assertTrue(update(NEAR.east()).isEmpty());
    assertTrue(described.isEmpty());
  }

  @Test
  void anUnchangedChunkIsLeftAloneWhileStandingStill() {
    place(NEAR);
    update(NEAR);
    described.clear();
    assertTrue(update(NEAR).isEmpty());
    assertTrue(described.isEmpty());
  }

  @Test
  void aChangedChunkIsSentAgain() {
    place(NEAR);
    update(NEAR);
    place(NEAR, NEAR.east());
    assertEquals(2, cellCount(update(NEAR)));
  }

  @Test
  void theLastCellLeavingRangeClearsTheChunk() {
    place(NEAR);
    update(NEAR);
    List<WireChunkPayload> updates = update(NEAR.up(RANGE + 1));
    assertEquals(List.of(new WireChunkPayload(CHUNK, List.of(), List.of())), updates);
  }

  @Test
  void aChunkLeftBehindClearsAndShowsAgainOnReturn() {
    place(NEAR);
    update(NEAR);
    BlockPos far = NEAR.east(16 * 8);
    assertEquals(List.of(new WireChunkPayload(CHUNK, List.of(), List.of())), update(far));
    assertEquals(1, cellCount(update(NEAR)));
  }
}
