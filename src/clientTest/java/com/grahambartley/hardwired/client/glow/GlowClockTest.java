package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowClockTest {

  @ParameterizedTest(name = "check at {0} after one at 100: {1}")
  @CsvSource({"100,false", "104,false", "105,true", "50,true"})
  void checksRunEveryFewTicksAndSurviveTimeGoingBack(long time, boolean due) {
    GlowClock clock = new GlowClock(5, 20);
    assertTrue(clock.checkDue(100));
    assertEquals(due, clock.checkDue(time));
  }

  @ParameterizedTest(name = "rebuild at {0}, changed {1}: {2}")
  @CsvSource({
    "100,false,false",
    "119,false,false",
    "120,false,true",
    "101,true,true",
    "50,false,true"
  })
  void rebuildsOnChangeOrWhenOld(long time, boolean changed, boolean due) {
    GlowClock clock = new GlowClock(5, 20);
    assertTrue(clock.rebuildDue(100, false));
    assertEquals(due, clock.rebuildDue(time, changed));
  }

  @Test
  void firstCallIsAlwaysDue() {
    GlowClock clock = new GlowClock(5, 20);
    assertTrue(clock.checkDue(0));
    assertTrue(clock.rebuildDue(0, false));
  }

  @Test
  void resetMakesTheNextCallsDueAgain() {
    GlowClock clock = new GlowClock(5, 20);
    assertTrue(clock.checkDue(100));
    assertTrue(clock.rebuildDue(100, false));
    clock.reset();
    assertTrue(clock.checkDue(100));
    assertTrue(clock.rebuildDue(100, false));
  }
}
