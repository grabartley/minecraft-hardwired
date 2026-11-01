package com.grahambartley.hardwired.block;

public final class LampFlicker {

  private static final int DIM_DIVISOR = 4;

  private LampFlicker() {}

  public static int next(int current, int wanted, boolean darkened) {
    int dimmed = wanted + (current - wanted) / DIM_DIVISOR;
    return darkened && wanted == 0 && dimmed > 0 && current > DIM_DIVISOR ? dimmed : wanted;
  }
}
