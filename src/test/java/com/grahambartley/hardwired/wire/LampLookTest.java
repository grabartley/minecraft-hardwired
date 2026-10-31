package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LampLookTest {

  @ParameterizedTest(name = "brightness {0} becomes {1}")
  @CsvSource({"-4,0", "0,0", "7,7", "15,15", "99,15"})
  void brightnessStaysInTheVanillaLightRange(int given, int expected) {
    assertEquals(expected, new LampLook(Hue.RED, given).brightness());
  }
}
