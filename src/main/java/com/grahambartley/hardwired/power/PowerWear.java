package com.grahambartley.hardwired.power;

public final class PowerWear {

  private PowerWear() {}

  public static int whole(double amount) {
    return (int) Math.floor(amount);
  }

  public static int workload(int load, int supply) {
    if (supply <= 0 || load <= 0) {
      return 0;
    }
    return (int) Math.min(15L, Math.max(1L, load * 15L / supply));
  }
}
