package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LampSettingsTest {

  @ParameterizedTest(name = "age rate {0} is {1}")
  @CsvSource({"-5,0", "0,0", "100,100", "5000,1000"})
  void ageRateIsClamped(int value, int expected) {
    assertEquals(expected, new LampSettings(value).ageRateMultiplierPercent());
  }
}
