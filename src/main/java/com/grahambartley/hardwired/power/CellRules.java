package com.grahambartley.hardwired.power;

public final class CellRules {

  public static final int MAX_LEVEL = 4;
  public static final int DUST_PER_REDSTONE_BLOCK = 9;

  private CellRules() {}

  public static int level(int charge, int capacity) {
    if (charge <= 0) {
      return 0;
    }
    long quarters = (charge * (long) MAX_LEVEL + capacity - 1) / capacity;
    return (int) Math.min(quarters, MAX_LEVEL);
  }

  public static boolean crossesEmpty(int before, int after) {
    return before > 0 != after > 0;
  }

  public static double partAfterBurn(int charge, double part, double step) {
    return charge > 0 ? part : Math.max(part, -step);
  }

  public static boolean takes(int charge, int capacity, int worth) {
    return itemsThatFit(charge, capacity, worth, 1) > 0;
  }

  public static int itemsThatFit(int charge, int capacity, int worth, int held) {
    if (worth <= 0 || charge >= capacity) {
      return 0;
    }
    return Math.min(held, (capacity - charge) / worth);
  }

  public static int worth(int dustCharge, boolean redstoneBlock) {
    return redstoneBlock ? dustCharge * DUST_PER_REDSTONE_BLOCK : dustCharge;
  }
}
