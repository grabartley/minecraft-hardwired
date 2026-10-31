package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CircuitActionTest {

  @ParameterizedTest(name = "{0} round trips")
  @EnumSource(CircuitAction.class)
  void actionsRoundTrip(CircuitAction action) {
    assertEquals(action, CircuitAction.byIndex(action.ordinal()));
  }

  @ParameterizedTest(name = "index {0} is no action")
  @ValueSource(ints = {-1, 4, Integer.MAX_VALUE, Integer.MIN_VALUE})
  void hostileIndexesAreNoAction(int index) {
    assertNull(CircuitAction.byIndex(index));
  }
}
