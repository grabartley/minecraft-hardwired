package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanCareTest {

  @ParameterizedTest(name = "tick speed {0} rate {1}%")
  @CsvSource({"3,100,0.000833333", "0,100,0", "3,0,0", "30,250,0.0208333"})
  void aSpanAgesAsOftenAsACopperBlockWouldEachSecond(int speed, int rate, double chance) {
    assertEquals(chance, SpanCare.ageChance(speed, rate), 1e-7);
  }
}
