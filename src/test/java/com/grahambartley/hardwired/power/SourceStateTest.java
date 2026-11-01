package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SourceStateTest {

  @ParameterizedTest(name = "closed {0} fuelled {1} burns fuel {2}")
  @CsvSource({
    "true,true,true,READY",
    "true,true,false,READY_FREE",
    "true,false,true,NO_FUEL",
    "true,false,false,NO_FUEL",
    "false,true,true,SWITCHED_OFF",
    "false,false,false,SWITCHED_OFF"
  })
  void aSourceNeedsItsSwitchThenItsFuel(
      boolean closed, boolean fuelled, boolean burnsFuel, SourceState state) {
    assertEquals(state, SourceState.of(closed, fuelled, burnsFuel));
  }

  @ParameterizedTest(name = "{0} ready {1}")
  @CsvSource({"READY,true", "READY_FREE,true", "NO_FUEL,false", "SWITCHED_OFF,false"})
  void bothReadyStatesCount(SourceState state, boolean ready) {
    assertEquals(ready, state.isReady());
  }

  @ParameterizedTest(name = "index {0}")
  @CsvSource({"-1,SWITCHED_OFF", "2,READY", "3,READY_FREE", "9,READY_FREE"})
  void aBadSavedIndexStaysInRange(int index, SourceState state) {
    assertEquals(state, SourceState.byIndex(index));
  }
}
