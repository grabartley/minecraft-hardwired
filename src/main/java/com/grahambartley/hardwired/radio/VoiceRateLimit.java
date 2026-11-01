package com.grahambartley.hardwired.radio;

public final class VoiceRateLimit {

  public static final double FRAMES_PER_SECOND = 12;
  public static final double BURST_FRAMES = 6;
  private static final double MILLIS_PER_SECOND = 1000;

  private double frames = BURST_FRAMES;
  private long lastMillis;

  public VoiceRateLimit(long startMillis) {
    lastMillis = startMillis;
  }

  public boolean allows(long nowMillis) {
    double earned = (nowMillis - lastMillis) * FRAMES_PER_SECOND / MILLIS_PER_SECOND;
    frames = Math.min(BURST_FRAMES, frames + Math.max(earned, 0));
    lastMillis = nowMillis;
    if (frames < 1) {
      return false;
    }
    frames--;
    return true;
  }
}
