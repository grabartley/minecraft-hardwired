package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CrackleClockTest {

  @ParameterizedTest(name = "strength {0} roll {1}")
  @CsvSource({"1,0.5,600", "1,0,300", "1,1,900", "0,0.5,100", "0,0,50", "0,1,150", "0.5,0.5,350"})
  void aGapComesFromSignalAndRoll(double strength, double roll, int ticks) {
    assertEquals(ticks, CrackleClock.gapTicks(strength, roll));
  }

  @ParameterizedTest(name = "roll {0}")
  @ValueSource(doubles = {0, 0.25, 0.75, 1})
  void weakerSignalMeansShorterGaps(double roll) {
    for (double strength = 0.1; strength <= 1; strength += 0.1) {
      assertTrue(
          CrackleClock.gapTicks(strength - 0.1, roll) < CrackleClock.gapTicks(strength, roll));
    }
  }

  @ParameterizedTest(name = "out of range strength {0}")
  @CsvSource({"-1,100", "2,600"})
  void strengthIsClamped(double strength, int ticks) {
    assertEquals(ticks, CrackleClock.gapTicks(strength, 0.5));
  }
}
