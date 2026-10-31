package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimelineRunNbtTest {

  private static final Timeline DOOR =
      new Timeline(List.of(new TimelineStep(1, 20), new TimelineStep(3, 20)), 100);

  private static Stream<Arguments> places() {
    return Stream.of(
        Arguments.of(0, null, 0, null, 0),
        Arguments.of(1, TimelineOrder.FORWARD, 7, null, 0),
        Arguments.of(1, TimelineOrder.FORWARD, 7, TimelineOrder.BACKWARD, 0),
        Arguments.of(1, TimelineOrder.BACKWARD, 3, TimelineOrder.STOP, 0),
        Arguments.of(2, null, 0, null, 55));
  }

  @ParameterizedTest(name = "position {0}, {1}, pending {3}")
  @MethodSource("places")
  void thePlaceSurvivesASave(
      int position, TimelineOrder travel, int ticksLeft, TimelineOrder pending, int reverseLeft) {
    TimelineRun run = TimelineRun.restored(DOOR, position, travel, ticksLeft, pending, reverseLeft);
    TimelineRun read = TimelineRunNbt.read(TimelineRunNbt.write(run), DOOR);
    assertEquals(run.position(), read.position());
    assertEquals(run.travel(), read.travel());
    assertEquals(run.ticksLeft(), read.ticksLeft());
    assertEquals(run.pending(), read.pending());
    assertEquals(run.reverseLeft(), read.reverseLeft());
  }

  @Test
  void aNewControllerHasNoSavedRun() {
    TimelineRun read = TimelineRunNbt.read(new NbtCompound(), DOOR);
    assertEquals(0, read.position());
    assertFalse(read.isRunning());
  }

  @Test
  void anUnknownOrderNameIsIgnored() {
    NbtCompound nbt = new NbtCompound();
    nbt.putString("travel", "SIDEWAYS");
    nbt.putInt("ticks_left", 4);
    assertFalse(TimelineRunNbt.read(nbt, DOOR).isRunning());
  }
}
