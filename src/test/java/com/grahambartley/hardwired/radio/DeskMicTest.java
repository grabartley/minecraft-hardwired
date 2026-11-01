package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DeskMicTest {

  private static final UUID FIRST = new UUID(0, 1);
  private static final UUID SECOND = new UUID(0, 2);

  @ParameterizedTest(name = "{0} voice {1}")
  @CsvSource({"RUNNING,true,true", "RUNNING,false,false", "READY,true,false", "NO_CELL,true,false"})
  void aDeskIsLiveOnlyOnAirWithVoiceAllowed(DeviceStatus status, boolean voice, boolean live) {
    assertEquals(live, DeskMic.mayBeLive(status, voice));
  }

  @ParameterizedTest(name = "pressing at {0}")
  @ValueSource(longs = {0, 1000})
  void twoPlayersGiveOneSpeaker(long now) {
    DeskMic mic = new DeskMic();
    assertTrue(mic.take(FIRST, now));
    assertFalse(mic.take(SECOND, now));
    assertEquals(FIRST, mic.speaker());
    assertFalse(mic.release(SECOND));
    assertTrue(mic.release(FIRST));
    assertNull(mic.speaker());
    assertTrue(mic.take(SECOND, now));
  }

  @ParameterizedTest(name = "{0} bytes accepted {1}")
  @CsvSource({"3,false", "4,true", "803,true", "804,false", "32767,false"})
  void framesAreLimitedInSize(int bytes, boolean accepted) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    assertEquals(accepted, mic.accepts(FIRST, bytes, 0));
  }

  @ParameterizedTest(name = "taken {0}")
  @ValueSource(booleans = {true, false})
  void onlyTheCurrentSpeakerIsHeard(boolean taken) {
    DeskMic mic = new DeskMic();
    if (taken) {
      mic.take(FIRST, 0);
    }
    assertFalse(mic.accepts(SECOND, 100, 0));
  }

  @ParameterizedTest(name = "flood of {0}")
  @ValueSource(ints = {7, 50})
  void aFloodIsDropped(int flood) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    int accepted = 0;
    for (int i = 0; i < flood; i++) {
      accepted += mic.accepts(FIRST, 100, 0) ? 1 : 0;
    }
    assertEquals(6, accepted);
  }

  @ParameterizedTest(name = "silent for {0} ms gave up {1}")
  @CsvSource({"3000,false", "3001,true"})
  void aSilentSpeakerLosesTheMic(long silent, boolean gaveUp) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    assertEquals(gaveUp, mic.gaveUp(silent));
  }

  @ParameterizedTest(name = "{0} take and release cycles")
  @ValueSource(ints = {1, 10})
  void takingAndReleasingDoesNotRefillTheBurst(int cycles) {
    DeskMic mic = new DeskMic();
    int accepted = 0;
    for (int i = 0; i < cycles; i++) {
      mic.take(FIRST, 0);
      for (int frame = 0; frame < 50; frame++) {
        accepted += mic.accepts(FIRST, 100, 0) ? 1 : 0;
      }
      mic.release(FIRST);
    }
    assertEquals(6, accepted);
  }

  @ParameterizedTest(name = "next speaker {0}")
  @ValueSource(booleans = {true, false})
  void aChangeOfSpeakerDoesNotRefillTheBurst(boolean sameSpeaker) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    for (int frame = 0; frame < 6; frame++) {
      mic.accepts(FIRST, 100, 0);
    }
    mic.clear();
    UUID next = sameSpeaker ? FIRST : SECOND;
    mic.take(next, 0);
    assertFalse(mic.accepts(next, 100, 0));
  }

  @ParameterizedTest(name = "pressing again at {0}")
  @ValueSource(longs = {1000, 2900})
  void pressingAgainDoesNotKeepASilentSpeakerOnTheMic(long pressedAgain) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    assertTrue(mic.take(FIRST, pressedAgain));
    assertTrue(mic.gaveUp(3001));
  }

  @ParameterizedTest(name = "cycled at {0}")
  @ValueSource(longs = {1000, 2900})
  void takingAFreeMicGivesTheSpeakerAFullWindow(long cycledAt) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    mic.release(FIRST);
    assertTrue(mic.take(FIRST, cycledAt));
    assertFalse(mic.gaveUp(cycledAt + DeskMic.SILENT_MILLIS));
    assertTrue(mic.gaveUp(cycledAt + DeskMic.SILENT_MILLIS + 1));
  }

  @ParameterizedTest(name = "heard at {0}")
  @CsvSource({"1000,4000,false", "1000,4001,true", "2500,5500,false", "2500,5501,true"})
  void onlyAnAcceptedFrameKeepsTheMic(long heardAt, long now, boolean gaveUp) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    assertTrue(mic.accepts(FIRST, 100, heardAt));
    assertEquals(gaveUp, mic.gaveUp(now));
  }

  @ParameterizedTest(name = "{0} bytes")
  @ValueSource(ints = {3, 804})
  void aRefusedFrameDoesNotKeepTheMic(int bytes) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    assertFalse(mic.accepts(FIRST, bytes, 2000));
    assertTrue(mic.gaveUp(3001));
  }

  @ParameterizedTest(name = "taken at {0}")
  @ValueSource(longs = {0, 100000})
  void aNewSpeakerHasTheWholeWindowToStart(long takenAt) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    mic.release(FIRST);
    mic.take(SECOND, takenAt);
    assertFalse(mic.gaveUp(takenAt + DeskMic.SILENT_MILLIS));
  }

  @ParameterizedTest(name = "back after {0} ms")
  @ValueSource(longs = {3001, 60000})
  void aSpeakerComingBackLaterHasTheWholeWindowAgain(long away) {
    DeskMic mic = new DeskMic();
    mic.take(FIRST, 0);
    mic.release(FIRST);
    mic.take(FIRST, away);
    assertFalse(mic.gaveUp(away + DeskMic.SILENT_MILLIS));
  }

  @ParameterizedTest(name = "shown {0}")
  @ValueSource(booleans = {true, false})
  void aClientOnlyShowsWhoIsTalking(boolean someone) {
    DeskMic mic = new DeskMic();
    mic.show(someone ? FIRST : null);
    assertEquals(someone ? FIRST : null, mic.speaker());
    assertFalse(mic.accepts(FIRST, 100, 0));
  }
}
