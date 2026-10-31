package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimelineStepTest {

  @ParameterizedTest(name = "{0} becomes {1}")
  @CsvSource({"-9, 2", "0, 2", "2, 2", "40, 40", "1200, 1200", "99999, 1200"})
  void waitStaysInBounds(int wait, int expected) {
    assertEquals(expected, new TimelineStep(0, wait).waitTicks());
  }

  @Test
  void outputsBeyondEightSlotsAreDropped() {
    assertEquals(0xFF, new TimelineStep(0xFFFF, 10).outputs());
  }

  @ParameterizedTest(name = "slot {0}")
  @ValueSource(ints = {0, 3, 7})
  void togglingFlipsOneSlot(int slot) {
    TimelineStep on = new TimelineStep(0, 10).toggled(slot);
    assertTrue(on.isOn(slot));
    assertFalse(on.toggled(slot).isOn(slot));
  }

  @Test
  void withWaitKeepsOutputs() {
    assertEquals(new TimelineStep(5, 30), new TimelineStep(5, 10).withWait(30));
  }
}
