package com.grahambartley.hardwired.power;

public final class PowerWear {

  public static final int MAX_WORKLOAD = 15;

  private PowerWear() {}

  public static int whole(double amount) {
    return (int) Math.floor(amount);
  }

  public static int workload(int load, int supply) {
    if (supply <= 0 || load <= 0) {
      return 0;
    }
    return (int) Math.min(MAX_WORKLOAD, Math.max(1L, load * (long) MAX_WORKLOAD / supply));
  }
}
