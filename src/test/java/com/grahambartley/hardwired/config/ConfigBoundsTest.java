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
}
