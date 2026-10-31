package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class SolarLogicTest {

  private static final float NOON = 0F;
  private static final float DUSK = (float) (Math.PI / 2);
  private static final float MIDNIGHT = (float) Math.PI;

  private static Stream<Arguments> lightCases() {
    return Stream.of(
        Arguments.of(15, 0, NOON, 15, true),
        Arguments.of(15, 3, NOON, 12, true),
        Arguments.of(7, 0, NOON, 7, false),
        Arguments.of(0, 0, NOON, 0, false),
        Arguments.of(15, 11, MIDNIGHT, 0, false),
        Arguments.of(15, 15, NOON, 0, false),
        Arguments.of(15, 0, DUSK, 5, false));
  }

  @ParameterizedTest(name = "sky {0} darkness {1} angle {2}")
  @MethodSource("lightCases")
  void daylightReadsLikeADaylightDetector(
      int sky, int darkness, float angle, int power, boolean day) {
    assertEquals(power, SolarLogic.daylight(sky, darkness, angle));
    assertEquals(day, SolarLogic.isDay(power));
  }

  @ParameterizedTest(name = "{0} per minute from tick {1}")
  @CsvSource({"6,0", "6,17", "1,1199", "7,600", "600,3", "0,0", "13,123456789012"})
  void aMinuteOfChecksEarnsExactlyThePerMinuteRate(int perMinute, long start) {
    int checks = 1200 / SolarLogic.CHECK_TICKS;
    int total =
        IntStream.range(0, checks)
            .map(
                check ->
                    SolarLogic.earned(
                        perMinute,
                        start + (long) check * SolarLogic.CHECK_TICKS,
                        SolarLogic.CHECK_TICKS))
            .sum();
    assertEquals(perMinute, total);
  }
}
