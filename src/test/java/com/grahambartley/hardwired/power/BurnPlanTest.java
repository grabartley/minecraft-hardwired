package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.PowerSettings;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BurnPlanTest {

  private static final double CLOSE = 1E-9;
  private static final double SECONDS_PER_MINUTE = 60;
  private static final int MINUTES_PER_DAY = 20;
  private static final int STACK = 64;
  private static final BurnRates RATES = new BurnRates(3, 1, 3);

  @ParameterizedTest(name = "load {0}")
  @CsvSource({"0,0,0.3", "6,0,0", "10,0.2,0", "22,0.8,0"})
  void fuelFreeSourcesCoverLoadFirstAndSpareEarns(int load, double cellBurn, double earned) {
    BurnPlan plan =
        BurnPlan.forSecond(
            List.of(
                new BurnPlan.Source(SourceKind.SOLAR, 6), new BurnPlan.Source(SourceKind.CELL, 16)),
            load,
            RATES);
    assertEquals(0D, plan.burnFor(0), CLOSE);
    assertEquals(cellBurn, plan.burnFor(1), CLOSE);
    assertEquals(earned, plan.earned(), CLOSE);
  }

  @ParameterizedTest(name = "load {0}")
  @CsvSource({"80", "40"})
  void fuelSourcesShareByRatingAndBurnAtTheirOwnRates(int load) {
    BurnPlan plan =
        BurnPlan.forSecond(
            List.of(
                new BurnPlan.Source(SourceKind.CELL, 16),
                new BurnPlan.Source(SourceKind.GENERATOR, 64)),
            load,
            RATES);
    assertEquals(load * 16D / 80 * 3 / 60, plan.burnFor(0), CLOSE);
    assertEquals(load * 64D / 80 * 1 / 60, plan.burnFor(1), CLOSE);
  }

  @ParameterizedTest(name = "spare rate {0} earns {1} a second")
  @CsvSource({"0,0", "6,0.6", "60,6"})
  void spareEarningsFollowTheirOwnRate(int spareRate, double earned) {
    BurnPlan plan =
        BurnPlan.forSecond(
            List.of(new BurnPlan.Source(SourceKind.DYNAMO, 6)), 0, new BurnRates(9, 9, spareRate));
    assertEquals(earned, plan.earned(), CLOSE);
  }

  private static double burnPerMinute(SourceKind kind, int load) {
    PowerSettings power = PowerSettings.DEFAULTS;
    return BurnPlan.forSecond(
                List.of(new BurnPlan.Source(kind, kind.rating(power))), load, power.burn())
            .burnFor(0)
        * SECONDS_PER_MINUTE;
  }

  @ParameterizedTest(name = "a stack of dust runs {1} load on a {0} for a game day")
  @CsvSource({"CELL,16", "GENERATOR,32"})
  void aStackOfDustRunsAHouseForADay(SourceKind kind, int load) {
    double minutes = STACK * PowerSettings.DEFAULTS.dustCharge() / burnPerMinute(kind, load);
    assertEquals(1D, minutes / MINUTES_PER_DAY, 0.05);
  }

  @ParameterizedTest(name = "a full {0} at its rating lasts {1} game days")
  @CsvSource({"CELL,1000,1.5", "GENERATOR,4000,3"})
  void aFullSourceAtItsRatingLasts(SourceKind kind, int capacity, double days) {
    double burn = burnPerMinute(kind, kind.rating(PowerSettings.DEFAULTS));
    assertEquals(days, capacity / burn / MINUTES_PER_DAY, 0.15);
  }
}
