package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ActionRateTest {

  private static final UUID PLAYER = new UUID(0, 1);
  private static final UUID OTHER = new UUID(0, 2);

  @ParameterizedTest(name = "acted at {0}, now {1}: allowed {2}")
  @CsvSource({
    "100, 100, false",
    "100, 103, false",
    "100, 104, true",
    "100, 5000, true",
    "100, 50, true"
  })
  void actionsAreSpacedOut(long first, long second, boolean allowed) {
    ActionRate rate = new ActionRate(4);
    assertTrue(rate.allow(PLAYER, first));
    assertEquals(allowed, rate.allow(PLAYER, second));
  }

  @Test
  void aRefusedActionDoesNotPushTheWindowBack() {
    ActionRate rate = new ActionRate(4);
    rate.allow(PLAYER, 100);
    rate.allow(PLAYER, 103);
    assertTrue(rate.allow(PLAYER, 104));
  }

  @Test
  void playersAreCountedApart() {
    ActionRate rate = new ActionRate(4);
    rate.allow(PLAYER, 100);
    assertTrue(rate.allow(OTHER, 100));
  }

  @Test
  void forgettingAPlayerClearsTheWait() {
    ActionRate rate = new ActionRate(4);
    rate.allow(PLAYER, 100);
    rate.forget(PLAYER);
    assertTrue(rate.allow(PLAYER, 100));
  }
}
