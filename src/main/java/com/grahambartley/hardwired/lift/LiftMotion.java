package com.grahambartley.hardwired.lift;

public final class LiftMotion {

  private static final int RESYNC_TICKS = 3;
  private static final double SETTLE_RATE = 0.5;
  private static final double SETTLED = 0.002;

  private int serial = -1;
  private int lastElapsed;
  private LiftTrip trip;
  private int ticks;
  private double heardLevel = Double.NaN;

  public void hear(double level) {
    heardLevel = level;
  }

  public void sync(int tripSerial, LiftTrip current, int elapsed) {
    if (tripSerial != serial) {
      serial = tripSerial;
      trip = current;
      ticks = elapsed;
      lastElapsed = elapsed;
    } else if (elapsed != lastElapsed) {
      lastElapsed = elapsed;
      if (Math.abs(elapsed - ticks) > RESYNC_TICKS) {
        ticks = elapsed;
      }
    }
  }

  public double next(double level) {
    if (trip != null) {
      ticks = Math.min(ticks + 1, trip.totalTicks());
      return trip.levelAt(ticks);
    }
    if (Double.isNaN(heardLevel)) {
      return level;
    }
    double gap = heardLevel - level;
    return Math.abs(gap) < SETTLED ? heardLevel : level + gap * SETTLE_RATE;
  }
}
