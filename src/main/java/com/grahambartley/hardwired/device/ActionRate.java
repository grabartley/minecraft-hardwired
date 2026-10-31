package com.grahambartley.hardwired.device;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActionRate {

  private final int minTicksBetween;
  private final Map<UUID, Long> lastAllowed = new HashMap<>();

  public ActionRate(int minTicksBetween) {
    this.minTicksBetween = minTicksBetween;
  }

  public boolean allow(UUID player, long now) {
    Long last = lastAllowed.get(player);
    if (last != null && now - last < minTicksBetween && now >= last) {
      return false;
    }
    lastAllowed.put(player, now);
    return true;
  }

  public void forget(UUID player) {
    lastAllowed.remove(player);
  }
}
