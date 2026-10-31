package com.grahambartley.hardwired.lift;

public final class LiftSafety {

  public static final int LOWERING_SLOWDOWN = 2;

  private static final double SEAT_OFFSET = 0.2;
  private static final int SEATS = 4;
  private static final int SLOW_FALL_BASE_TICKS = 100;
  private static final int SLOW_FALL_TICKS_PER_BLOCK = 4;

  private LiftSafety() {}

  public static double seatX(int passenger) {
    return passenger % SEATS % 3 == 0 ? -SEAT_OFFSET : SEAT_OFFSET;
  }

  public static double seatZ(int passenger) {
    return passenger % SEATS < 2 ? -SEAT_OFFSET : SEAT_OFFSET;
  }

  public static int slowFallTicks(double drop) {
    return SLOW_FALL_BASE_TICKS + (int) Math.ceil(Math.max(drop, 0)) * SLOW_FALL_TICKS_PER_BLOCK;
  }

  public static boolean inColumn(double x, double z, int columnX, int columnZ) {
    return Math.floor(x) == columnX && Math.floor(z) == columnZ;
  }
}
