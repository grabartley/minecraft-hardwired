package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftSettingsTest {

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,low", "2000000,high"})
  void everyValueIsClamped(int value, String end) {
    LiftSettings s = new LiftSettings(value);
    boolean low = end.equals("low");
    assertEquals(
        low ? LiftSettings.TICKS_PER_BLOCK_MIN : LiftSettings.TICKS_PER_BLOCK_MAX,
        s.ticksPerBlock());
  }

  @ParameterizedTest(name = "default {0}")
  @CsvSource({"20"})
  void defaultsSitInsideTheirBounds(int first) {
    assertEquals(first, LiftSettings.DEFAULTS.ticksPerBlock());
    assertEquals(LiftSettings.DEFAULTS, new LiftSettings(LiftSettings.DEFAULTS.ticksPerBlock()));
  }
}
