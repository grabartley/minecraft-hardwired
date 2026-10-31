package com.grahambartley.hardwired.wire;

public final class TripDetector {

  public static final int MAX_CHANGES = 10;
  public static final int WINDOW_TICKS = 20;

  private final long[] changeTicks = new long[MAX_CHANGES + 1];
  private int next;
  private int recorded;

  public boolean recordChange(long tick) {
    if (recorded > 0 && changeTicks[Math.floorMod(next - 1, changeTicks.length)] == tick) {
      return false;
    }
    changeTicks[next] = tick;
    next = (next + 1) % changeTicks.length;
    recorded = Math.min(recorded + 1, changeTicks.length);
    return recorded == changeTicks.length && tick - changeTicks[next] < WINDOW_TICKS;
  }

  public void reset() {
    next = 0;
    recorded = 0;
  }
}
