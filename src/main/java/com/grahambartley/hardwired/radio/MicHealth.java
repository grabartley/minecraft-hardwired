package com.grahambartley.hardwired.radio;

public final class MicHealth {

  private static final int DEAD_FRAMES = 10;

  private int deadFrames;

  public boolean refused(short[] frame) {
    deadFrames = isDead(frame) ? deadFrames + 1 : 0;
    return deadFrames >= DEAD_FRAMES;
  }

  private static boolean isDead(short[] frame) {
    for (short sample : frame) {
      if (sample != 0) {
        return false;
      }
    }
    return true;
  }
}
