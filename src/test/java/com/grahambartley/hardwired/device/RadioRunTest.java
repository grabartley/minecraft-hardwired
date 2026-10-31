package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioRunTest {

  @ParameterizedTest(name = "disc {0} ended {1} speakers {2}")
  @CsvSource({
    "false,false,3,NO_DISC",
    "false,true,3,NO_DISC",
    "true,true,3,SONG_ENDED",
    "true,false,0,NO_SPEAKER",
    "true,false,1,RUNNING",
    "true,false,2,RUNNING"
  })
  void aRadioPlaysOnlyAFreshDiscIntoASpeaker(
      boolean disc, boolean ended, int speakers, DeviceStatus status) {
    assertEquals(status, RadioRun.demand(disc, ended, speakers));
  }

  @ParameterizedTest(name = "{0} of {1}")
  @CsvSource({"0,100,false", "99,100,false", "100,100,true", "119,100,true"})
  void theSongEndsOnceItsLengthHasPlayed(int played, int song, boolean ended) {
    assertEquals(ended, RadioRun.songEnded(played, song));
  }

  @ParameterizedTest(name = "{0} at {1}")
  @CsvSource({
    "40,RUNNING,60",
    "40,SONG_ENDED,40",
    "40,NO_CHARGE,0",
    "40,NO_SPEAKER,0",
    "40,NO_DISC,0"
  })
  void anInterruptedSongStartsAgainFromTheTop(int played, DeviceStatus status, int after) {
    assertEquals(after, RadioRun.playedAfter(played, status, 20));
  }
}
