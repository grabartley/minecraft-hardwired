package com.grahambartley.hardwired.client.radio;

public final class TalkState {

  public enum CaptureStep {
    KEEP,
    START,
    STOP
  }

  private static final int ASK_AGAIN_TICKS = 10;

  private boolean held;
  private boolean down;
  private boolean focused = true;
  private int ticksSinceAsked;

  public boolean press(boolean available) {
    if (down || !available) {
      return false;
    }
    down = true;
    held = true;
    ticksSinceAsked = 0;
    return true;
  }

  public boolean release() {
    down = false;
    return drop();
  }

  public boolean drop() {
    boolean wasHeld = held;
    held = false;
    return wasHeld;
  }

  public boolean mustDrop(boolean nowFocused, boolean available, boolean inReach) {
    boolean lostFocus = focused && !nowFocused;
    focused = nowFocused;
    return lostFocus || !available || !inReach;
  }

  public boolean askAgain(boolean micFree) {
    if (!held || !micFree || ++ticksSinceAsked < ASK_AGAIN_TICKS) {
      return false;
    }
    ticksSinceAsked = 0;
    return true;
  }

  public CaptureStep capture(boolean capturing, boolean mine) {
    if (capturing) {
      return mine ? CaptureStep.KEEP : CaptureStep.STOP;
    }
    return held && mine ? CaptureStep.START : CaptureStep.KEEP;
  }
}
