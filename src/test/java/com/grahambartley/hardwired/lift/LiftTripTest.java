package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftTripTest {

  private static final double CLOSE = 1.0E-6;

  @ParameterizedTest(name = "{0} to {1} at {2} ticks")
  @CsvSource({
    "0,6,6,6,36",
    "6,0,6,6,36",
    "0,1,2,1,2",
    "10,13,40,3,120",
    "2.5,0,6,3,15",
    "3.25,4,4,1,3"
  })
  void aTripTakesTicksPerBlockForEachBlock(
      double from, double to, int pace, int blocks, int ticks) {
    LiftTrip trip = new LiftTrip(from, to, pace);
    assertEquals(blocks, trip.blocks());
    assertEquals(ticks, trip.totalTicks());
    assertFalse(trip.arrivedBy(ticks - 1));
    assertTrue(trip.arrivedBy(ticks));
  }

  @ParameterizedTest(name = "{0} to {1} at {2} ticks")
  @CsvSource({"0,6,6", "6,0,6", "0,1,2", "0,2,3", "5,4,40", "2.5,0,6"})
  void aTripStartsAndEndsExactlyAndNeverOvershoots(double from, double to, int pace) {
    LiftTrip trip = new LiftTrip(from, to, pace);
    assertEquals(from, trip.levelAt(0), CLOSE);
    assertEquals(to, trip.levelAt(trip.totalTicks()), CLOSE);
    double low = Math.min(from, to) - CLOSE;
    double high = Math.max(from, to) + CLOSE;
    IntStream.rangeClosed(0, trip.totalTicks())
        .mapToDouble(trip::levelAt)
        .forEach(level -> assertTrue(level >= low && level <= high, "level " + level));
  }

  @ParameterizedTest(name = "{0} to {1} at {2} ticks")
  @CsvSource({"0,6,6", "6,0,6", "0,10,2", "0,1,40"})
  void aTripNeverJerks(double from, double to, int pace) {
    LiftTrip trip = new LiftTrip(from, to, pace);
    double cruise = trip.distance() / (trip.totalTicks() - Math.min(8, trip.totalTicks() / 2));
    for (int tick = 0; tick < trip.totalTicks(); tick++) {
      double step = (trip.levelAt(tick + 1) - trip.levelAt(tick)) * trip.direction();
      assertTrue(step >= 0, "went backwards at " + tick);
      assertTrue(step <= cruise + CLOSE, "too fast at " + tick + ": " + step);
    }
  }

  @ParameterizedTest(name = "tick {0}")
  @CsvSource({"0,0", "1,1", "36,6"})
  void blocksStartAsTheCarEntersThem(int ticks, int started) {
    assertEquals(started, new LiftTrip(0, 6, 6).blocksStartedBy(ticks));
  }

  @ParameterizedTest(name = "{0} to {1}")
  @CsvSource({"0,6,6", "6,0,6", "2.5,0,6", "0,1,2"})
  void everyBlockIsStartedExactlyOnceInOrder(double from, double to, int pace) {
    LiftTrip trip = new LiftTrip(from, to, pace);
    int seen = 0;
    for (int tick = 1; tick <= trip.totalTicks(); tick++) {
      int started = trip.blocksStartedBy(tick);
      assertTrue(started == seen || started == seen + 1, "skipped a block at " + tick);
      seen = started;
    }
    assertEquals(trip.blocks(), seen);
  }

  @ParameterizedTest(name = "block {3} of {0} to {1}")
  @CsvSource({"0,6,1,1", "6,0,1,5", "2.5,0,3,0", "2.5,0,1,1.5", "0,1,1,1"})
  void endOfBlockStopsAtTheTarget(double from, double to, int block, double end) {
    assertEquals(end, new LiftTrip(from, to, 6).endOfBlock(block), CLOSE);
  }
}
