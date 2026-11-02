package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GlowBuildSlotTest {

  @Test
  void startsEmptyAndIdle() {
    GlowBuildSlot<String> slot = new GlowBuildSlot<>("empty");
    assertEquals("empty", slot.current());
    assertFalse(slot.isBusy());
  }

  @ParameterizedTest(name = "builds {0}")
  @ValueSource(strings = {"one", "two"})
  void finishedBuildReplacesTheCurrentValue(String value) {
    GlowBuildSlot<String> slot = new GlowBuildSlot<>("empty");
    slot.build(() -> value, Runnable::run);
    assertEquals(value, slot.current());
    assertFalse(slot.isBusy());
  }

  @Test
  void unfinishedBuildKeepsTheOldValueAndStaysBusy() {
    GlowBuildSlot<String> slot = new GlowBuildSlot<>("empty");
    List<Runnable> queued = new ArrayList<>();
    slot.build(() -> "new", queued::add);
    assertEquals("empty", slot.current());
    assertTrue(slot.isBusy());
    queued.forEach(Runnable::run);
    assertEquals("new", slot.current());
  }

  @Test
  void clearDropsABuildStillRunning() {
    GlowBuildSlot<String> slot = new GlowBuildSlot<>("empty");
    List<Runnable> queued = new ArrayList<>();
    slot.build(() -> "stale", queued::add);
    slot.clear();
    queued.forEach(Runnable::run);
    assertEquals("empty", slot.current());
    assertFalse(slot.isBusy());
  }
}
