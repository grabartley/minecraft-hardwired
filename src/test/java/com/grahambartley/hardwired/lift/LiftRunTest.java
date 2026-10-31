package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LiftRunTest {

  private static LiftRun reload(LiftRun run) {
    NbtCompound nbt = new NbtCompound();
    run.write(nbt);
    LiftRun loaded = new LiftRun();
    loaded.read(nbt);
    return loaded;
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = LiftPhase.class,
      names = {"MOVING", "LOWERING"})
  void aTripSurvivesASaveAtTheSamePlace(LiftPhase phase) {
    LiftRun run = new LiftRun();
    run.begin(new LiftTrip(0, 6, 6), phase);
    run.startBlock(2);
    for (int tick = 0; tick < 10; tick++) {
      run.advance();
    }
    run.queue().add(3);
    run.rememberGates(List.of(new BlockPos(1, 2, 3)));
    LiftRun loaded = reload(run);
    assertEquals(phase, loaded.phase());
    assertEquals(run.trip(), loaded.trip());
    assertEquals(10, loaded.ticks());
    assertEquals(2, loaded.blocksStarted());
    assertEquals(run.level(), loaded.level());
    assertArrayEquals(new int[] {3}, loaded.queue().toArray());
    assertEquals(run.knownGates(), loaded.knownGates());
  }

  @Test
  void aStopSurvivesASaveWithItsReasonAndClearsCalls() {
    LiftRun run = new LiftRun();
    run.queue().add(9);
    run.stop(DeviceStatus.NO_CHARGE, 2);
    LiftRun loaded = reload(run);
    assertEquals(LiftPhase.STOPPED, loaded.phase());
    assertEquals(DeviceStatus.NO_CHARGE, loaded.stopReason());
    assertTrue(loaded.queue().isEmpty());
    assertTrue(loaded.pausing());
    assertTrue(loaded.pausing());
    assertFalse(loaded.pausing());
  }

  @Test
  void aSavedTravelPhaseWithNoTripRests() {
    NbtCompound nbt = new NbtCompound();
    nbt.putString("phase", "MOVING");
    LiftRun loaded = new LiftRun();
    loaded.read(nbt);
    assertEquals(LiftPhase.RESTING, loaded.phase());
    assertNull(loaded.trip());
  }

  @Test
  void aRestingCarIsAtOnlyItsOwnFloor() {
    LiftRun run = new LiftRun();
    run.rest(5, 0);
    assertTrue(run.restingAt(5));
    assertFalse(run.restingAt(4));
    run.begin(new LiftTrip(5, 6, 6), LiftPhase.MOVING);
    assertFalse(run.restingAt(5));
  }

  @Test
  void everyChangeOfTripIsANewSerialForClients() {
    LiftRun run = new LiftRun();
    int first = run.serial();
    run.begin(new LiftTrip(0, 1, 2), LiftPhase.MOVING);
    int second = run.serial();
    run.rest(1, 0);
    assertTrue(first < second && second < run.serial());
  }
}
