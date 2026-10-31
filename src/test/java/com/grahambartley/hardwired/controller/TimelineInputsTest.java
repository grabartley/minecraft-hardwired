package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimelineInputsTest {

  private static Stream<Arguments> oneColour() {
    return Stream.of(
        Arguments.of(false, false, null),
        Arguments.of(false, true, TimelineOrder.FORWARD),
        Arguments.of(true, true, null),
        Arguments.of(true, false, TimelineOrder.BACKWARD));
  }

  @ParameterizedTest(name = "was {0}, now {1}: {2}")
  @MethodSource("oneColour")
  void oneColourRunsForwardsOnAndBackwardsOff(boolean was, boolean now, TimelineOrder expected) {
    assertEquals(expected, TimelineInputs.orderFor(true, was, was, now, now));
  }

  private static Stream<Arguments> twoColours() {
    return Stream.of(
        Arguments.of(false, false, true, false, TimelineOrder.FORWARD),
        Arguments.of(false, false, false, true, TimelineOrder.BACKWARD),
        Arguments.of(false, false, true, true, TimelineOrder.BACKWARD),
        Arguments.of(true, false, false, false, null),
        Arguments.of(false, true, false, false, null),
        Arguments.of(true, true, true, true, null));
  }

  @ParameterizedTest(name = "open {0} to {2}, close {1} to {3}: {4}")
  @MethodSource("twoColours")
  void twoColoursActOnlyWhenTurnedOn(
      boolean wasOpen, boolean wasClose, boolean open, boolean close, TimelineOrder expected) {
    assertEquals(expected, TimelineInputs.orderFor(false, wasOpen, wasClose, open, close));
  }
}
