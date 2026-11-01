package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FrequencyTest {

  @ParameterizedTest(name = "{0} snaps to {1}")
  @CsvSource({
    "530,530",
    "0,530",
    "-50,530",
    "1600,1600",
    "9999,1600",
    "534,530",
    "535,540",
    "777,780",
    "1000,1000"
  })
  void aDialSettingSnapsToTheBandInTens(int kilohertz, int expected) {
    assertEquals(expected, Frequency.snap(kilohertz));
  }

  @ParameterizedTest(name = "{0} and {1} are {2} steps apart")
  @CsvSource({"1000,1000,0", "1000,1010,1", "1010,1000,1", "530,1600,107"})
  void stepsCountBothWays(int first, int second, int steps) {
    assertEquals(steps, Frequency.stepsApart(first, second));
  }

  @ParameterizedTest(name = "band of {0} steps")
  @CsvSource({"107"})
  void theBandIsTheNumberOfStepsAcrossIt(int steps) {
    assertEquals(steps, Frequency.stepsAcross());
  }
}
