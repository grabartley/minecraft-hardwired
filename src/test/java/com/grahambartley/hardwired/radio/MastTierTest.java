package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MastTierTest {

  @ParameterizedTest(name = "over tier {0} comes tier {1}")
  @CsvSource({"0,1", "1,2", "2,3", "3,4", "4,4"})
  void aMastNarrowsOverTheOneBelowUntilItIsSlim(int below, int expected) {
    assertEquals(expected, MastTier.aboveMast(below));
  }

  @ParameterizedTest(name = "tier {0} is inset {1}")
  @CsvSource({"0,1", "4,5"})
  void higherTiersStandThinner(int tier, int inset) {
    assertEquals(inset, MastTier.insetPixels(tier));
  }
}
