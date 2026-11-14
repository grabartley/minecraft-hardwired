package com.grahambartley.hardwired.screen;

public final class RotaryWheel {

  public static final double STEP = Math.toRadians(30);
  public static final double STOP = Math.toRadians(60);
  public static final int MOST_PULSES = 10;
  private static final double FULL = 2 * Math.PI;

  private RotaryWheel() {}

  public static int pulsesFor(int digit) {
    return digit == 0 ? MOST_PULSES : digit;
  }

  public static int digitFor(int pulses) {
    return pulses % MOST_PULSES;
  }

  public static double holeAngle(int pulses) {
    return STOP - (pulses + 1) * STEP;
  }

  public static double travel(int pulses) {
    return (pulses + 1) * STEP;
  }

  public static int holeAt(double dx, double dy, int ringRadius, int holeRadius) {
    for (int pulses = 1; pulses <= MOST_PULSES; pulses++) {
      double angle = holeAngle(pulses);
      double hx = Math.cos(angle) * ringRadius;
      double hy = Math.sin(angle) * ringRadius;
      if (Math.hypot(dx - hx, dy - hy) <= holeRadius) {
        return pulses;
      }
    }
    return 0;
  }

  public static double turn(double fromAngle, double toAngle, int pulses) {
    double delta = Math.floorMod(Math.round(Math.toDegrees(toAngle - fromAngle) * 1000), 360_000);
    double turned = Math.toRadians(delta / 1000);
    double limit = travel(pulses);
    if (turned <= limit) {
      return turned;
    }
    return turned < limit + (FULL - limit) / 2 ? limit : 0;
  }

  public static boolean atStop(double turned, int pulses) {
    return turned >= travel(pulses) - Math.toRadians(1);
  }
}
