package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftMotionTest {

  private static final LiftTrip TRIP = new LiftTrip(0, 6, 6);
  private static final double CLOSE = 1.0E-6;

  @Test
  void onATripTheClientRunsTheSameCurveAsTheServer() {
    LiftMotion motion = new LiftMotion();
    motion.sync(1, TRIP, 0);
    double level = 0;
    for (int tick = 1; tick <= TRIP.totalTicks(); tick++) {
      motion.hear(tick % 3 == 0 ? level - 1 : level + 1);
      level = motion.next(level);
      assertEquals(TRIP.levelAt(tick), level, CLOSE, "tick " + tick);
    }
  }

  @Test
  void aTripHoldsAtItsEndUntilTheServerMovesOn() {
    LiftMotion motion = new LiftMotion();
    motion.sync(1, TRIP, TRIP.totalTicks());
    assertEquals(6, motion.next(6), CLOSE);
    assertEquals(6, motion.next(6), CLOSE);
  }

  @ParameterizedTest(name = "elapsed {0}")
  @CsvSource({"20,20", "2,0", "40,40"})
  void aJoiningClientPicksUpMidTrip(int elapsed, int joinedAt) {
    LiftMotion motion = new LiftMotion();
    motion.sync(1, TRIP, joinedAt);
    motion.sync(1, TRIP, elapsed);
    double expected = Math.abs(elapsed - joinedAt) > 3 ? elapsed : joinedAt;
    assertEquals(TRIP.levelAt((int) expected + 1), motion.next(0), CLOSE);
  }

  @Test
  void withNoTripTheCarSettlesOnWhatTheServerSays() {
    LiftMotion motion = new LiftMotion();
    motion.sync(1, null, 0);
    motion.hear(4);
    double level = 3;
    for (int tick = 0; tick < 20; tick++) {
      level = motion.next(level);
    }
    assertEquals(4, level);
  }

  @Test
  void aStopBetweenFloorsEndsTheTripAtOnce() {
    LiftMotion motion = new LiftMotion();
    motion.sync(1, TRIP, 0);
    motion.hear(2);
    motion.sync(2, null, 0);
    assertEquals(1.5, motion.next(1));
  }

  @Test
  void beforeAnyNewsTheCarStaysPut() {
    LiftMotion motion = new LiftMotion();
    assertEquals(7.5, motion.next(7.5));
  }
}
