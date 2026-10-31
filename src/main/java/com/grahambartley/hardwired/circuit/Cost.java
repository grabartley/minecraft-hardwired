package com.grahambartley.hardwired.circuit;

public final class Cost {

  private Cost() {}

  public static int redstone(int parts, int partsPerRedstone) {
    return Math.max(1, Math.ceilDiv(parts, partsPerRedstone));
  }
}
