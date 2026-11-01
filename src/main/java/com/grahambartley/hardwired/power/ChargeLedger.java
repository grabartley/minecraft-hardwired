package com.grahambartley.hardwired.power;

import java.util.List;

public final class ChargeLedger {

  private ChargeLedger() {}

  public static int fill(List<? extends ChargeStore> stores, int amount) {
    int remaining = Math.max(amount, 0);
    for (ChargeStore store : stores) {
      remaining -= store.fill(remaining);
    }
    return amount - remaining;
  }
}
