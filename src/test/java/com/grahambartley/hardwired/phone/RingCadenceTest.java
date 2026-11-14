package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RingCadenceTest {

  @ParameterizedTest(name = "tick {0}")
  @CsvSource({"0,true", "39,true", "40,false", "119,false", "120,true", "159,true", "160,false"})
  void aSingleLineRingsTwoSecondsOnAndFourOff(long tick, boolean ringing) {
    assertEquals(ringing, RingCadence.ringing(tick, false));
  }

  @ParameterizedTest(name = "tick {0}")
  @CsvSource({
    "0,true",
    "23,true",
    "24,false",
    "32,true",
    "39,true",
    "40,false",
    "48,true",
    "56,false",
    "100,false",
    "120,true"
  })
  void aPartyLineRingsOneLongAndTwoShort(long tick, boolean ringing) {
    assertEquals(ringing, RingCadence.ringing(tick, true));
  }

  @ParameterizedTest(name = "tick {0} party {1}")
  @CsvSource({
    "0,false,40",
    "1,false,0",
    "120,false,40",
    "0,true,24",
    "32,true,8",
    "48,true,8",
    "40,true,0"
  })
  void eachStrokeStartsOnceWithItsLength(long tick, boolean party, int length) {
    assertEquals(length, RingCadence.strokeStarting(tick, party));
  }

  @ParameterizedTest(name = "tick {0} party {1} strikes {2}")
  @CsvSource({
    "0,false,1",
    "1,false,0",
    "3,false,1.19",
    "6,false,1",
    "39,false,1.19",
    "42,false,0",
    "24,true,0",
    "33,true,1.19"
  })
  void theBellTrillsTwoGongsOnlyWhileItRings(long tick, boolean party, float pitch) {
    assertEquals(pitch, RingCadence.strikePitch(tick, party), 1e-6);
  }
}
