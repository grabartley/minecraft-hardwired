package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VanillaPowerTest {

  @ParameterizedTest(name = "{0} at y {1} starts at y {2}")
  @CsvSource({"lower, 5, 5, 6", "upper, 6, 5, 6"})
  void aDoorListsBothHalvesLowestFirst(String half, int y, int first, int second) {
    BlockState door =
        Blocks.OAK_DOOR
            .getDefaultState()
            .with(
                Properties.DOUBLE_BLOCK_HALF,
                half.equals("upper") ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    assertEquals(
        List.of(new BlockPos(1, first, 1), new BlockPos(1, second, 1)),
        VanillaPower.cells(new BlockPos(1, y, 1), door));
  }

  @ParameterizedTest(name = "a lever at y {0} is one cell")
  @CsvSource({"5", "60"})
  void aSingleBlockIsOneCell(int y) {
    BlockPos pos = new BlockPos(1, y, 1);
    assertEquals(List.of(pos), VanillaPower.cells(pos, Blocks.LEVER.getDefaultState()));
  }

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
