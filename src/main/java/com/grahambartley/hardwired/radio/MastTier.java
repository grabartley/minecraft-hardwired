package com.grahambartley.hardwired.radio;

public final class MastTier {

  public static final int FOOT = 0;
  public static final int SLIM = 4;
  private static final int FOOT_INSET = 1;

  private MastTier() {}

  public static int aboveMast(int tierBelow) {
    return Math.min(tierBelow + 1, SLIM);
  }

  public static int insetPixels(int tier) {
    return FOOT_INSET + tier;
  }
}
