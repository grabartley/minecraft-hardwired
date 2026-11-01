package com.grahambartley.hardwired.radio;

public final class CrackleClock {

  public static final int SWEEP_GAP_TICKS = 16;
  private static final double FULL_SIGNAL_GAP_TICKS = 240;
  private static final double NO_SIGNAL_GAP_TICKS = 24;
  private static final double SHORTEST_SHARE = 0.5;

  private CrackleClock() {}

  public static int gapTicks(double strength, double unitRandom) {
    double signal = Math.clamp(strength, 0, 1);
    double mean = NO_SIGNAL_GAP_TICKS + (FULL_SIGNAL_GAP_TICKS - NO_SIGNAL_GAP_TICKS) * signal;
    return (int) Math.round(mean * (SHORTEST_SHARE + Math.clamp(unitRandom, 0, 1)));
  }
}
