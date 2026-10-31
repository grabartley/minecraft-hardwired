package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VanillaPowerTest {

  @ParameterizedTest
  @CsvSource({
    "8, 8, 0, 0, true",
    "8, 8, 1, 0, true",
    "8, 8, -1, 0, true",
    "8, 8, 2, 0, false",
    "8, 8, 0, -2, false",
    "13, 8, 1, 1, true",
    "13, 8, 2, 0, false",
    "14, 8, 2, 0, true",
    "1, 8, -2, 0, true",
    "2, 8, -2, 0, false",
  })
  void aChunkLoadRefreshesEveryTouchThatReadsIntoIt(
      int x, int z, int chunkX, int chunkZ, boolean expected) {
    assertEquals(
        expected, VanillaPower.readsFrom(new BlockPos(x, 64, z), new ChunkPos(chunkX, chunkZ)));
  }
}
