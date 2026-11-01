package com.grahambartley.hardwired.radio;

public final class LayerWeight {

  public static final double TOUGHEST = 8;
  private static final double STONE_HARDNESS = 1.5;

  private LayerWeight() {}

  public static double of(float hardness) {
    return hardness < 0 ? TOUGHEST : Math.min(hardness / STONE_HARDNESS, TOUGHEST);
  }
}
