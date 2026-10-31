package com.grahambartley.hardwired.power;

import java.util.List;

public final class ChargeLedger {

  private ChargeLedger() {}

  public static int available(List<? extends ChargeStore> stores) {
    return stores.stream().mapToInt(ChargeStore::charge).sum();
  }

  public static boolean tryDrain(List<? extends ChargeStore> stores, int amount) {
    if (available(stores) < amount) {
      return false;
    }
    int remaining = amount;
    for (ChargeStore store : stores) {
      remaining -= store.drain(remaining);
    }
    return remaining == 0;
  }

  public static int fill(List<? extends ChargeStore> stores, int amount) {
    int remaining = Math.max(amount, 0);
    for (ChargeStore store : stores) {
      remaining -= store.fill(remaining);
    }
    return amount - remaining;
  }
}
