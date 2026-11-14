package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DialStateTest {

  private static OptionalInt dialTo(DialState state, int pulses, double degrees) {
    double from = RotaryWheel.holeAngle(pulses);
    state.grab(pulses, from, false);
    state.drag(from + Math.toRadians(degrees));
    return state.release();
  }

  private static int clicksUntilStill(DialState state) {
    int clicks = 0;
    for (int tick = 0; tick < 100; tick++) {
      if (state.tick()) {
        clicks++;
      }
    }
    return clicks;
  }

  @ParameterizedTest(name = "{0} pulses")
  @ValueSource(ints = {1, 2, 5, 9, 10})
  void aDigitTurnedToTheStopClicksOncePerPulseOnTheWayBack(int pulses) {
    DialState state = new DialState();
    OptionalInt digit = dialTo(state, pulses, Math.toDegrees(RotaryWheel.travel(pulses)));
    assertEquals(OptionalInt.of(pulses % 10), digit);
    assertEquals(pulses, clicksUntilStill(state));
    assertEquals(0, state.turned(), 1e-9);
  }

  @ParameterizedTest(name = "{0} pulses short by {1} degrees")
  @CsvSource({"3,10", "10,45", "1,59"})
  void aDigitLetGoShortOfTheStopDialsNothingAndSpringsBackSilently(int pulses, double short_) {
    DialState state = new DialState();
    double degrees = Math.toDegrees(RotaryWheel.travel(pulses)) - short_;
    assertTrue(dialTo(state, pulses, degrees).isEmpty());
    assertEquals(0, clicksUntilStill(state));
    assertEquals("", state.dialed());
  }

  @ParameterizedTest(name = "digits {0}")
  @CsvSource({"1,0,5,105", "9,9,9,999", "2,1,0,210"})
  void threeDigitsMakeANumber(int a, int b, int c, int number) {
    DialState state = new DialState();
    for (int digit : new int[] {a, b, c}) {
      int pulses = digit == 0 ? 10 : digit;
      dialTo(state, pulses, Math.toDegrees(RotaryWheel.travel(pulses)));
      clicksUntilStill(state);
    }
    assertEquals(OptionalInt.of(number), state.number());
    assertFalse(state.canGrab(false));
  }

  @ParameterizedTest(name = "attempt {0}")
  @ValueSource(ints = {1, 2, 3})
  void clearingTheDigitsAfterAFailedCallLetsThePlayerDialAgainEveryTime(int attempts) {
    DialState state = new DialState();
    for (int attempt = 0; attempt < attempts; attempt++) {
      for (int i = 0; i < DialState.DIGITS; i++) {
        dialTo(state, 4, Math.toDegrees(RotaryWheel.travel(4)));
        clicksUntilStill(state);
      }
      assertFalse(state.canGrab(false));
      state.clearDigits();
      assertTrue(state.canGrab(false));
    }
  }

  @ParameterizedTest(name = "on call {0}")
  @ValueSource(booleans = {true, false})
  void theWheelCannotBeGrabbedOnACall(boolean onCall) {
    assertEquals(!onCall, new DialState().grab(2, 0, onCall));
  }

  @ParameterizedTest(name = "{0} pulses")
  @ValueSource(ints = {1, 6})
  void theWheelCannotBeGrabbedUntilItHasWoundBack(int pulses) {
    DialState state = new DialState();
    dialTo(state, pulses, Math.toDegrees(RotaryWheel.travel(pulses)));
    assertFalse(state.canGrab(false));
    clicksUntilStill(state);
    assertTrue(state.canGrab(false));
  }
}
