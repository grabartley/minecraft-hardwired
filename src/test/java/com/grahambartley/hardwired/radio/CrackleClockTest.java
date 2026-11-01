package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CrackleClockTest {

  @ParameterizedTest(name = "strength {0} roll {1}")
  @CsvSource({"1,0.5,240", "1,0,120", "1,1,360", "0,0.5,24", "0,0,12", "0,1,36", "0.5,0.5,132"})
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
  @CsvSource({"-1,24", "2,240"})
  void strengthIsClamped(double strength, int ticks) {
    assertEquals(ticks, CrackleClock.gapTicks(strength, 0.5));
  }
}
