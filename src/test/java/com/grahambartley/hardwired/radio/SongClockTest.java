package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SongClockTest {

  @ParameterizedTest(name = "tick {0} of a song from {1} is {2} ms")
  @CsvSource({"100,100,0", "120,100,1000", "1300,100,60000", "90,100,0"})
  void everyListenerWorksOutTheSameMomentFromWorldTime(long now, long start, long millis) {
    assertEquals(millis, SongClock.millisIn(now, start));
  }

  @ParameterizedTest(name = "tick {0} from {1} of {2}: {3}")
  @CsvSource({"100,100,50,false", "149,100,50,false", "150,100,50,true", "900,100,50,true"})
  void aSongEndsAtItsLength(long now, long start, long length, boolean ended) {
    assertEquals(ended, SongClock.ended(now, start, length));
  }

  @ParameterizedTest(name = "paused at {1} ticks in, resumed at {0}")
  @CsvSource({"1000,0,1000", "1000,300,700", "50,50,0", "5000,4999,1"})
  void resumingKeepsTheSameMoment(long now, long place, long start) {
    assertEquals(start, SongClock.startFor(now, place));
    assertEquals(place, SongClock.placeAt(now, start));
  }

  @org.junit.jupiter.api.Test
  void noPlaceStartsFromTheTop() {
    assertEquals(700L, SongClock.startFor(700, SongClock.NO_PLACE));
  }

  @ParameterizedTest(name = "now {0}, started {1}")
  @CsvSource({"100,200,0", "200,200,0", "201,200,1"})
  void aPlaceIsNeverNegative(long now, long start, long place) {
    assertEquals(place, SongClock.placeAt(now, start));
  }
}
