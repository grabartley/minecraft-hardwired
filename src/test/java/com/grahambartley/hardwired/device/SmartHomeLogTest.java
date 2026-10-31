package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SmartHomeLogTest {

  private static final UUID ALICE = new UUID(1, 1);
  private static final UUID BOB = new UUID(2, 2);

  private static SmartHomeLog empty() {
    return SmartHomeLog.read(new NbtCompound());
  }

  @ParameterizedTest(name = "{0} repeats")
  @ValueSource(ints = {1, 2, 9})
  void aRepeatedDeviceCountsOnce(int repeats) {
    SmartHomeLog log = empty();
    int count = 0;
    for (int i = 0; i < repeats; i++) {
      count = log.record(ALICE, "lamp");
    }
    assertEquals(1, count);
  }

  @Test
  void eachPlayerCountsAlone() {
    SmartHomeLog log = empty();
    log.record(ALICE, "lamp");
    log.record(ALICE, "switch");
    assertEquals(1, log.record(BOB, "lamp"));
  }

  @Test
  void theCountStopsAtTheGoal() {
    SmartHomeLog log = empty();
    int count = 0;
    for (int device = 0; device < SmartHomeLog.GOAL + 5; device++) {
      count = log.record(ALICE, "device" + device);
    }
    assertEquals(SmartHomeLog.GOAL, count);
  }

  @Test
  void onlyANewDeviceDirtiesTheLog() {
    SmartHomeLog log = empty();
    log.record(ALICE, "lamp");
    assertTrue(log.isDirty());
    log.setDirty(false);
    log.record(ALICE, "lamp");
    assertFalse(log.isDirty());
  }

  @Test
  void theCountSurvivesASave() {
    SmartHomeLog log = empty();
    log.record(ALICE, "lamp");
    log.record(ALICE, "switch");
    SmartHomeLog read = SmartHomeLog.read(log.writeNbt(new NbtCompound(), null));
    assertEquals(2, read.record(ALICE, "lamp"));
    assertEquals(3, read.record(ALICE, "controller"));
  }
}
