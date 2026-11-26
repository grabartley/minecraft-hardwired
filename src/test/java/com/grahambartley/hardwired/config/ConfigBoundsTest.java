package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConfigBoundsTest {

  @ParameterizedTest(name = "{0} in {1}..{2} is {3}")
  @CsvSource({"5,0,10,5", "-1,0,10,0", "11,0,10,10", "0,0,10,0", "10,0,10,10"})
  void clampsToTheRange(int value, int min, int max, int expected) {
    assertEquals(expected, ConfigBounds.clamp(value, min, max));
  }

  @ParameterizedTest(name = "min {0} with max {1} becomes {2}")
  @CsvSource({"5,32,5", "32,32,32", "40,33,33", "64,1,1", "1,128,1"})
  void aMinimumAboveItsMaximumIsLoweredToIt(int min, int max, int expected) {
    assertEquals(expected, ConfigBounds.orderedMin("min", min, "max", max));
  }
}
