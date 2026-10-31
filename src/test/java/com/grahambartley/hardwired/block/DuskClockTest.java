package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DuskClockTest {

  @ParameterizedTest(name = "time {0} dark: {1}")
  @CsvSource({
    "0,false",
    "6000,false",
    "12499,false",
    "12500,true",
    "18000,true",
    "23499,true",
    "23500,false",
    "36000,false",
    "42000,true"
  })
  void darkFromDuskUntilDawn(long time, boolean dark) {
    assertEquals(dark, DuskClock.isDark(time));
  }
}
