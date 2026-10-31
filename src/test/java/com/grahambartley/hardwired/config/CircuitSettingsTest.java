package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CircuitSettingsTest {

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,low", "2000000,high"})
  void everyValueIsClamped(int value, String end) {
    int[] v = new int[2];
    java.util.Arrays.fill(v, value);
    CircuitSettings s = new CircuitSettings(v[0], v[1]);
    boolean low = end.equals("low");
    assertEquals(low ? CircuitSettings.MAX_PARTS_MIN : CircuitSettings.MAX_PARTS_MAX, s.maxParts());
    assertEquals(
        low ? CircuitSettings.PARTS_PER_REDSTONE_MIN : CircuitSettings.PARTS_PER_REDSTONE_MAX,
        s.partsPerRedstone());
  }

  @ParameterizedTest(name = "default {0}")
  @CsvSource({"64"})
  void defaultsSitInsideTheirBounds(int first) {
    assertEquals(first, CircuitSettings.DEFAULTS.maxParts());
    assertEquals(
        CircuitSettings.DEFAULTS,
        new CircuitSettings(
            CircuitSettings.DEFAULTS.maxParts(), CircuitSettings.DEFAULTS.partsPerRedstone()));
  }
}
