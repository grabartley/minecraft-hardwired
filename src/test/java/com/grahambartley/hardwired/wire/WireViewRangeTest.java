package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WireViewRangeTest {

  private static final BlockPos VIEWER = new BlockPos(8, 64, 8);

  @ParameterizedTest(name = "offset {0} {1} {2} in range {3}: {4}")
  @CsvSource({
    "32, 0, 0, 32, true",
    "33, 0, 0, 32, false",
    "0, 32, 0, 32, true",
    "0, 33, 0, 32, false",
    "0, 0, -33, 32, false",
    "23, 23, 0, 32, false",
    "22, 22, 0, 32, true",
    "8, 0, 0, 8, true",
    "9, 0, 0, 8, false"
  })
  void wireCountsOnlyWithinTheRangeInAnyDirection(
      int x, int y, int z, int range, boolean expected) {
    assertEquals(expected, WireViewRange.contains(VIEWER, VIEWER.add(x, y, z).asLong(), range));
  }

  @ParameterizedTest(name = "range {0} at x {1}")
  @CsvSource({"8, 0", "8, 15", "32, 0", "32, 15", "33, 0", "33, 15", "96, 7"})
  void theChunkSquareReachesTheFarthestBlockInRange(int range, int x) {
    int radius = WireViewRange.chunkRadius(range);
    assertTrue(((x + range) >> 4) <= radius);
    assertTrue(((x - range) >> 4) >= -radius);
  }

  @ParameterizedTest(name = "range {0} keeps {1} cells and {2} marks")
  @CsvSource({"8, 1, 1", "20, 2, 1", "40, 2, 2"})
  void aPayloadKeepsOnlyWhatIsInRange(int range, int cells, int marks) {
    WireChunkPayload payload =
        new WireChunkPayload(
            5L,
            List.of(cell(VIEWER.up(2)), cell(VIEWER.up(16))),
            List.of(new MarkView(VIEWER.up().asLong(), true), mark(VIEWER.up(30))));
    WireChunkPayload filtered = WireViewRange.filter(payload, VIEWER, range);
    assertEquals(5L, filtered.chunk());
    assertEquals(cells, filtered.cells().size());
    assertEquals(marks, filtered.marks().size());
  }

  private static CellView cell(BlockPos pos) {
    return new CellView(pos.asLong(), 1, 0, 0, 0);
  }

  private static MarkView mark(BlockPos pos) {
    return new MarkView(pos.asLong(), false);
  }

  @ParameterizedTest(name = "{0} chunks")
  @CsvSource({"0,0", "1,16", "7,112", "33,528"})
  void chunksConvertToBlocks(int chunks, int blocks) {
    assertEquals(blocks, WireViewRange.blocksIn(chunks));
  }
}
