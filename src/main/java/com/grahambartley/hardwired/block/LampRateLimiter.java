package com.grahambartley.hardwired.block;

public final class LampRateLimiter {

  public static final int MIN_TICKS_BETWEEN_CHANGES = 7;

  private LampRateLimiter() {}

  public static long ticksUntilAllowed(long lastChangeTick, long now) {
    return Math.max(0, lastChangeTick + MIN_TICKS_BETWEEN_CHANGES - now);
  }
}
