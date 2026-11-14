package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.phone.LineStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LineTonesTest {

  @ParameterizedTest(name = "{0} at tick {1}")
  @CsvSource({
    "RINGING_OUT,0,RINGBACK",
    "RINGING_OUT,1,NONE",
    "RINGING_OUT,60,NONE",
    "RINGING_OUT,120,RINGBACK",
    "BUSY,0,BUSY",
    "BUSY,5,NONE",
    "BUSY,10,BUSY",
    "DIAL_TONE,0,NONE",
    "CONNECTED,0,NONE",
    "NO_ANSWER,10,NONE"
  })
  void eachLineStatePlaysItsOwnTone(LineStatus status, long ticks, LineTones.Tone tone) {
    assertEquals(tone, LineTones.toneAt(status, ticks));
  }
}
