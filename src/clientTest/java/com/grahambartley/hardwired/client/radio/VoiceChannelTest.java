package com.grahambartley.hardwired.client.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.radio.VoiceJitter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceChannelTest {

  private static final int FRAME_BYTES = 3200;

  private static byte[] frame() {
    return new byte[FRAME_BYTES];
  }

  @ParameterizedTest(name = "{0} ms")
  @ValueSource(longs = {0, 1000, 100000})
  void aChannelNoOneHasSpokenOnIsOffAir(long now) {
    assertEquals(false, new VoiceChannel().onAir(now));
  }

  @ParameterizedTest(name = "frame at {0}, now {1}")
  @CsvSource({"500,500,true", "500,2499,true", "500,2500,false", "0,0,true"})
  void aVoiceStaysOnAirForItsTail(long frameAt, long now, boolean onAir) {
    VoiceChannel channel = new VoiceChannel();
    channel.receive(frame(), frameAt);
    assertEquals(onAir, channel.onAir(now));
  }

  @ParameterizedTest(name = "{0} frames heard, {1} kept")
  @CsvSource({"1,1", "3,3", "4,3", "9,3"})
  void aLateTapStartsWithTheLastThreeFrames(int heard, int kept) {
    VoiceChannel channel = new VoiceChannel();
    for (int i = 0; i < heard; i++) {
      channel.receive(frame(), i);
    }
    assertEquals(kept * FRAME_BYTES, channel.attach().held());
  }

  @ParameterizedTest(name = "{0} frames")
  @ValueSource(ints = {4, 8})
  void everyAttachedTapHearsEachFrame(int frames) {
    VoiceChannel channel = new VoiceChannel();
    VoiceJitter first = channel.attach();
    VoiceJitter second = channel.attach();
    for (int i = 0; i < frames; i++) {
      channel.receive(frame(), i);
    }
    assertEquals(frames * FRAME_BYTES, first.held());
    assertEquals(frames * FRAME_BYTES, second.held());
  }

  @ParameterizedTest(name = "{0} frames")
  @ValueSource(ints = {4, 8})
  void aDetachedTapHearsNoMore(int frames) {
    VoiceChannel channel = new VoiceChannel();
    VoiceJitter tap = channel.attach();
    channel.detach(tap);
    for (int i = 0; i < frames; i++) {
      channel.receive(frame(), i);
    }
    assertEquals(0, tap.held());
  }
}
