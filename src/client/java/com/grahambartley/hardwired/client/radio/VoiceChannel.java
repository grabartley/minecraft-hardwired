package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.VoiceFormat;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class VoiceChannel {

  private static final long TAIL_MILLIS = 2000;
  private static final int RECENT_FRAMES = 3;
  private static final int PREFILL_BYTES = VoiceFormat.millisToBytes(350);
  private static final int MOST_BYTES = VoiceFormat.millisToBytes(1500);

  private final List<VoiceJitter> taps = new ArrayList<>();
  private final Deque<byte[]> recent = new ArrayDeque<>();
  private long lastFrame = -TAIL_MILLIS;

  void receive(byte[] pcm, long nowMillis) {
    lastFrame = nowMillis;
    recent.addLast(pcm);
    if (recent.size() > RECENT_FRAMES) {
      recent.removeFirst();
    }
    taps.forEach(tap -> tap.offer(pcm));
  }

  boolean onAir(long nowMillis) {
    return nowMillis - lastFrame < TAIL_MILLIS;
  }

  VoiceJitter attach() {
    VoiceJitter tap = new VoiceJitter(PREFILL_BYTES, MOST_BYTES);
    recent.forEach(tap::offer);
    taps.add(tap);
    return tap;
  }

  void detach(VoiceJitter tap) {
    taps.remove(tap);
  }
}
