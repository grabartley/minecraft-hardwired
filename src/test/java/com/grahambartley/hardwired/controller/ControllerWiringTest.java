package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ControllerWiringTest {

  private static ControllerWiring wiring(Hue open, Hue close, Hue... outputs) {
    return new ControllerWiring(
        open, close, java.util.Arrays.stream(outputs).map(Optional::of).toList());
  }

  @Test
  void slotsArePaddedToEight() {
    assertEquals(TimelineStep.OUTPUT_SLOTS, wiring(Hue.PLAIN, Hue.PLAIN, Hue.RED).outputs().size());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Hue.class,
      names = {"PLAIN", "WHITE"})
  void aReadColourCannotAlsoBeDriven(Hue read) {
    ControllerWiring wiring = wiring(Hue.PLAIN, Hue.WHITE, read, Hue.RED);
    assertEquals(Optional.empty(), wiring.outputs().get(0));
    assertEquals(Optional.of(Hue.RED), wiring.outputs().get(1));
  }

  @Test
  void aColourDrivesFromOneSlotOnly() {
    ControllerWiring wiring = wiring(Hue.PLAIN, Hue.PLAIN, Hue.RED, Hue.RED);
    assertEquals(Optional.of(Hue.RED), wiring.outputs().get(0));
    assertEquals(Optional.empty(), wiring.outputs().get(1));
  }

  @ParameterizedTest(name = "mask {0}, {1}: {2}")
  @CsvSource({
    "0, RED, false",
    "1, RED, true",
    "1, BLUE, false",
    "2, BLUE, true",
    "3, BLUE, true",
    "3, LIME, false"
  })
  void drivingFollowsTheMask(int mask, Hue color, boolean driven) {
    assertEquals(driven, wiring(Hue.PLAIN, Hue.PLAIN, Hue.RED, Hue.BLUE).drives(mask, color));
  }

  @Test
  void oneColourIsOnlyWhenBothMatch() {
    assertTrue(ControllerWiring.DEFAULT.readsOneColour());
    assertFalse(wiring(Hue.RED, Hue.BLUE).readsOneColour());
  }

  @Test
  void theDefaultLeavesRoomForAFirstDoor() {
    assertEquals(
        List.of(Hue.RED, Hue.BLUE, Hue.LIME, Hue.YELLOW),
        ControllerWiring.DEFAULT.outputs().stream().flatMap(Optional::stream).toList());
  }
}
