package com.grahambartley.hardwired.lift;

public final class LiftSafety {

  public static final int LOWERING_SLOWDOWN = 2;

  private static final double REACH_BELOW_FLOOR = 0.5;
  private static final double REACH_ABOVE_FLOOR = 1.3;
  private static final int SLOW_FALL_BASE_TICKS = 100;
  private static final int SLOW_FALL_TICKS_PER_BLOCK = 4;

  private LiftSafety() {}

  public static boolean inCar(
      double x, double y, double z, double carX, double floorY, double carZ, int span) {
    double half = span / 2.0;
    return Math.abs(x - carX) < half
        && Math.abs(z - carZ) < half
        && y >= floorY - REACH_BELOW_FLOOR
        && y <= floorY + REACH_ABOVE_FLOOR;
  }

  public static int slowFallTicks(double drop) {
    return SLOW_FALL_BASE_TICKS + (int) Math.ceil(Math.max(drop, 0)) * SLOW_FALL_TICKS_PER_BLOCK;
  }

  public static boolean inColumn(double x, double z, int columnX, int columnZ, int span) {
    double east = Math.floor(x) - columnX;
    double south = Math.floor(z) - columnZ;
    return east >= 0 && east < span && south >= 0 && south < span;
  }
}
