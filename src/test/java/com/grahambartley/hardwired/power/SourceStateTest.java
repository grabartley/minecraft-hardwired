package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceStateTest {

  @ParameterizedTest(name = "closed {0} fuelled {1}")
  @CsvSource({
    "true,true,READY",
    "true,false,NO_FUEL",
    "false,true,SWITCHED_OFF",
    "false,false,SWITCHED_OFF"
  })
  void aSourceNeedsItsSwitchThenItsFuel(boolean closed, boolean fuelled, SourceState state) {
    assertEquals(state, SourceState.of(closed, fuelled));
  }

  @ParameterizedTest(name = "index {0}")
  @CsvSource({"-1,SWITCHED_OFF", "2,READY", "9,READY"})
  void aBadSavedIndexStaysInRange(int index, SourceState state) {
    assertEquals(state, SourceState.byIndex(index));
  }
}
