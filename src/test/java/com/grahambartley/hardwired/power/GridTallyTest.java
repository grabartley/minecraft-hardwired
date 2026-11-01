package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GridTallyTest {

  private static Long2IntOpenHashMap facts(int... pairs) {
    Long2IntOpenHashMap map = new Long2IntOpenHashMap();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
    return map;
  }

  @ParameterizedTest(name = "{0} and {1} count as {2}")
  @CsvSource({"4,4,4", "2,8,8", "8,2,8", "0,5,5"})
  void aDeviceOnTwoNetworksCountsOnce(int first, int second, int total) {
    Long2IntOpenHashMap merged = GridTally.loads(List.of(facts(1, first), facts(1, second)));
    assertEquals(total, GridTally.total(merged));
  }

  @ParameterizedTest(name = "{0} and {1} add to {2}")
  @CsvSource({"4,4,8", "2,8,10"})
  void differentDevicesAddUp(int first, int second, int total) {
    assertEquals(
        total, GridTally.total(GridTally.loads(List.of(facts(1, first), facts(2, second)))));
  }

  @ParameterizedTest(name = "{0} waiting {1} master open {2}")
  @CsvSource({
    "READY, false, false, true",
    "READY, true, false, false",
    "READY, false, true, false",
    "NO_FUEL, false, false, false",
    "SWITCHED_OFF, false, false, false"
  })
  void aSourceRunsOnlyReadyAndUnheld(
      SourceState state, boolean waiting, boolean masterOpen, boolean runs) {
    assertEquals(runs, GridTally.runs(state, waiting, masterOpen));
  }

  @ParameterizedTest(name = "master open {0}, waiting {1}")
  @CsvSource({
    "false, false, 24, 1, false",
    "false, true, 8, 1, true",
    "true, false, 0, 3, false",
    "true, true, 0, 3, true"
  })
  void countAddsSupplyFromRunningSourcesOnly(
      boolean masterOpen, boolean waiting, int supply, int switchedOff, boolean starting) {
    Long2IntOpenHashMap sources =
        facts(
            1,
            SourceState.READY.ordinal(),
            2,
            SourceState.READY.ordinal(),
            3,
            SourceState.SWITCHED_OFF.ordinal());
    Long2IntOpenHashMap ratings = facts(1, 8, 2, 16, 3, 64);
    LongSet held = waiting ? new LongOpenHashSet(new long[] {2L}) : new LongOpenHashSet();
    GridRule.Count count = GridTally.count(facts(9, 5), ratings, sources, held, masterOpen);
    assertEquals(new GridRule.Count(5, supply, 3, switchedOff, starting), count);
  }
}
