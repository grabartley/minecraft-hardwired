package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CopperCareTest {

  @ParameterizedTest(name = "{0} percent")
  @CsvSource({"100,0.0568889", "200,0.1137778", "0,0"})
  void theAgeChanceScalesWithTheSetting(int percent, float expected) {
    assertEquals(expected, CopperCare.ageChance(percent), 1e-5F);
  }
}
