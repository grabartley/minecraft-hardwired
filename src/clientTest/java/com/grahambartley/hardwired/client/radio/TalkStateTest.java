package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.client.radio.TalkState.CaptureStep;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TalkStateTest {

  @ParameterizedTest(name = "available {0}")
  @CsvSource({"true,true", "false,false"})
  void pressingTakesTheMicOnlyWhenTheDeskCanTalk(boolean available, boolean asks) {
    assertEquals(asks, new TalkState().press(available));
  }

  @ParameterizedTest(name = "{0} repeats")
  @ValueSource(ints = {1, 30})
  void keyRepeatsWhileDownAreIgnored(int repeats) {
    TalkState state = new TalkState();
    assertTrue(state.press(true));
    for (int i = 0; i < repeats; i++) {
      assertFalse(state.press(true));
    }
  }

  @ParameterizedTest(name = "{0} repeats")
  @ValueSource(ints = {1, 30})
  void aFailedMicIsNotRetriedUntilARealRelease(int repeats) {
    TalkState state = new TalkState();
    state.press(true);
    assertTrue(state.drop());
    for (int i = 0; i < repeats; i++) {
      assertFalse(state.press(true));
    }
    assertFalse(state.release());
    assertTrue(state.press(true));
  }

  @ParameterizedTest(name = "{0} presses while unavailable")
  @ValueSource(ints = {1, 5})
  void aPressHeldThroughAnUnavailableDeskTakesTheMicOnceItIsAvailable(int presses) {
    TalkState state = new TalkState();
    for (int i = 0; i < presses; i++) {
      assertFalse(state.press(false));
    }
    assertTrue(state.press(true));
  }

  @ParameterizedTest(name = "pressed {0}")
  @CsvSource({"true,true", "false,false"})
  void onlyAHeldMicIsGivenBack(boolean pressed, boolean givesBack) {
    TalkState state = new TalkState();
    state.press(pressed);
    assertEquals(givesBack, state.release());
    assertFalse(state.release());
  }

  @ParameterizedTest(name = "focused {0} available {1} reach {2}")
  @CsvSource({
    "true,true,true,false",
    "false,true,true,true",
    "true,false,true,true",
    "true,true,false,true"
  })
  void losingFocusTheDeskOrReachDropsTheMic(
      boolean focused, boolean available, boolean inReach, boolean drops) {
    assertEquals(drops, new TalkState().mustDrop(focused, available, inReach));
  }

  @ParameterizedTest(name = "tick {0}")
  @ValueSource(ints = {2, 3})
  void onlyLosingFocusCountsNotStayingUnfocused(int ticks) {
    TalkState state = new TalkState();
    state.mustDrop(false, true, true);
    for (int i = 0; i < ticks; i++) {
      assertFalse(state.mustDrop(false, true, true));
    }
  }

  @ParameterizedTest(name = "every {0} ticks")
  @ValueSource(ints = {10})
  void aHeldPressAsksAgainEveryTenTicksWhileTheMicIsFree(int every) {
    TalkState state = new TalkState();
    state.press(true);
    for (int round = 0; round < 3; round++) {
      for (int i = 1; i < every; i++) {
        assertFalse(state.askAgain(true));
      }
      assertTrue(state.askAgain(true));
    }
  }

  @ParameterizedTest(name = "held {0} free {1}")
  @CsvSource({"true,false", "false,true", "false,false"})
  void nothingIsAskedUnlessHeldWithAFreeMic(boolean held, boolean free) {
    TalkState state = new TalkState();
    state.press(held);
    for (int i = 0; i < 25; i++) {
      assertFalse(state.askAgain(free));
    }
  }

  @ParameterizedTest(name = "capturing {0} mine {1} held {2}")
  @CsvSource({
    "true,true,true,KEEP",
    "true,true,false,KEEP",
    "true,false,true,STOP",
    "true,false,false,STOP",
    "false,true,true,START",
    "false,true,false,KEEP",
    "false,false,true,KEEP",
    "false,false,false,KEEP"
  })
  void theMicOpensOnlyForAHeldPressWhileTheDeskSaysItIsMine(
      boolean capturing, boolean mine, boolean held, CaptureStep step) {
    TalkState state = new TalkState();
    state.press(held);
    assertEquals(step, state.capture(capturing, mine));
  }
}
