package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StartOrderTest {

  private static final BlockPos MASTER = BlockPos.ORIGIN;

  @ParameterizedTest(name = "now {0}")
  @CsvSource({"0", "1000"})
  void theNearestSourceStartsFirstAndTheLastInOneSecond(long now) {
    BlockPos near = new BlockPos(1, 0, 0);
    BlockPos middle = new BlockPos(5, 0, 0);
    BlockPos far = new BlockPos(9, 0, 0);
    Long2LongOpenHashMap starts = StartOrder.schedule(List.of(far, near, middle), MASTER, now);
    assertTrue(starts.get(near.asLong()) < starts.get(middle.asLong()));
    assertTrue(starts.get(middle.asLong()) < starts.get(far.asLong()));
    assertEquals(now + StartOrder.START_TICKS, starts.get(far.asLong()));
  }

  @ParameterizedTest(name = "{0} sources")
  @CsvSource({"0", "1", "40"})
  void everySourceGetsAStart(int count) {
    List<BlockPos> sources =
        java.util.stream.IntStream.range(0, count).mapToObj(i -> new BlockPos(i, 0, 0)).toList();
    assertEquals(count, StartOrder.schedule(sources, MASTER, 0).size());
  }
}
