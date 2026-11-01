package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BurnPlanTest {

  private static final double CLOSE = 1E-9;

  @ParameterizedTest(name = "load {0}")
  @CsvSource({"0,0,0.3", "6,0,0", "10,0.2,0", "22,0.8,0"})
  void fuelFreeSourcesCoverLoadFirstAndSpareEarns(int load, double cellBurn, double earned) {
    BurnPlan plan =
        BurnPlan.forSecond(
            List.of(
                new BurnPlan.Source(SourceKind.SOLAR, 6), new BurnPlan.Source(SourceKind.CELL, 16)),
            load,
            3);
    assertEquals(0D, plan.burnFor(0), CLOSE);
    assertEquals(cellBurn, plan.burnFor(1), CLOSE);
    assertEquals(earned, plan.earned(), CLOSE);
  }

  @ParameterizedTest(name = "load {0}")
  @CsvSource({"80", "40"})
  void fuelSourcesShareByRatingAndGeneratorsBurnHalf(int load) {
    BurnPlan plan =
        BurnPlan.forSecond(
            List.of(
                new BurnPlan.Source(SourceKind.CELL, 16),
                new BurnPlan.Source(SourceKind.GENERATOR, 64)),
            load,
            3);
    assertEquals(load * 16D / 80 * 3 / 60, plan.burnFor(0), CLOSE);
    assertEquals(load * 64D / 80 * 3 / 60 / 2, plan.burnFor(1), CLOSE);
  }
}
