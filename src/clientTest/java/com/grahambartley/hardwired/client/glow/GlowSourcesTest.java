package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.light.Glow;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GlowSourcesTest {

  private static Glow glowAt(int x) {
    return new Glow(new BlockPos(x, 0, 0), 15, 0xFF0000);
  }

  @ParameterizedTest(name = "limit {0} keeps {1} glows")
  @CsvSource({"1,1", "2,2", "3,3", "9,3"})
  void keepsTheNearest(int limit, int kept) {
    List<Glow> all = List.of(glowAt(30), glowAt(10), glowAt(20));
    List<Glow> nearestFirst = List.of(glowAt(10), glowAt(20), glowAt(30));
    assertEquals(nearestFirst.subList(0, kept), GlowSources.nearest(all, BlockPos.ORIGIN, limit));
  }

  @ParameterizedTest(name = "input order flipped: {0}")
  @ValueSource(booleans = {true, false})
  void equalDistancesGiveTheSameListWhateverTheInputOrder(boolean flipped) {
    List<Glow> input = flipped ? List.of(glowAt(5), glowAt(-5)) : List.of(glowAt(-5), glowAt(5));
    assertEquals(
        GlowSources.nearest(List.of(glowAt(5), glowAt(-5)), BlockPos.ORIGIN, 2),
        GlowSources.nearest(input, BlockPos.ORIGIN, 2));
  }
}
