package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class TimelineRunTest {

  private static final int WAIT = 4;
  private static final Timeline DOOR =
      new Timeline(List.of(new TimelineStep(1, WAIT), new TimelineStep(3, WAIT)), 0);
  private static final Timeline SELF_CLOSING = new Timeline(DOOR.steps(), 10);

  private static List<Integer> outputsOver(TimelineRun run, Timeline timeline, int ticks) {
    List<Integer> seen = new ArrayList<>();
    for (int tick = 0; tick < ticks; tick++) {
      int outputs = run.outputs(timeline);
      if (seen.isEmpty() || seen.get(seen.size() - 1) != outputs) {
        seen.add(outputs);
      }
      run.tick(timeline);
    }
    return seen;
  }

  private static TimelineRun finishedForward(Timeline timeline) {
    TimelineRun run = new TimelineRun();
    run.order(timeline, TimelineOrder.FORWARD);
    for (int tick = 0; tick < 2 * WAIT; tick++) {
      run.tick(timeline);
    }
    return run;
  }

  @ParameterizedTest(name = "dark order {0} holds position {1}")
  @CsvSource({"FORWARD,0,1", "BACKWARD,0,0", "STOP,0,0"})
  void anOrderOnADarkGridWaitsForPower(TimelineOrder order, int held, int afterPower) {
    TimelineRun run = new TimelineRun();
    run.orderDark(DOOR, order);
    assertEquals(held, run.position());
    run.tick(DOOR);
    assertEquals(afterPower, run.position());
  }

  @Test
  void theLatestDarkOrderWins() {
    TimelineRun run = finishedForward(DOOR);
    run.orderDark(DOOR, TimelineOrder.FORWARD);
    run.orderDark(DOOR, TimelineOrder.BACKWARD);
    run.tick(DOOR);
    assertEquals(1, run.position());
  }

  @Test
  void aDarkOrderMidStepFollowsTheStep() {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    run.orderDark(DOOR, TimelineOrder.BACKWARD);
    assertEquals(1, run.position());
    assertEquals(TimelineOrder.BACKWARD, run.pending());
  }

  @Test
  void aDarkOrderSurvivesAReload() {
    TimelineRun run = new TimelineRun();
    run.orderDark(DOOR, TimelineOrder.FORWARD);
    TimelineRun restored =
        TimelineRun.restored(
            DOOR, run.position(), run.travel(), run.ticksLeft(), run.pending(), run.reverseLeft());
    restored.tick(DOOR);
    assertEquals(1, restored.position());
  }

  @Test
  void forwardsSetsEachStepInOrderThenRests() {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    assertEquals(List.of(1, 3), outputsOver(run, DOOR, 20));
    assertFalse(run.isRunning());
    assertEquals(2, run.position());
  }

  @Test
  void backwardsUndoesEachStepInReverseAndEndsWithEverythingOff() {
    TimelineRun run = finishedForward(DOOR);
    run.order(DOOR, TimelineOrder.BACKWARD);
    assertEquals(List.of(1, 0), outputsOver(run, DOOR, 20));
    assertFalse(run.isRunning());
    assertEquals(0, run.position());
  }

  private static Stream<Arguments> uselessOrders() {
    return Stream.of(
        Arguments.of(0, TimelineOrder.BACKWARD),
        Arguments.of(0, TimelineOrder.STOP),
        Arguments.of(2, TimelineOrder.FORWARD),
        Arguments.of(2, TimelineOrder.STOP));
  }

  @ParameterizedTest(name = "position {0}, {1}")
  @MethodSource("uselessOrders")
  void anOrderWithNowhereToGoLeavesTheControllerIdle(int position, TimelineOrder order) {
    TimelineRun run = TimelineRun.restored(DOOR, position, null, 0, null, 0);
    run.order(DOOR, order);
    assertFalse(run.isRunning());
    assertEquals(position, run.position());
  }

  @Test
  void anEmptyTimelineNeverRuns() {
    TimelineRun run = new TimelineRun();
    run.order(Timeline.EMPTY, TimelineOrder.FORWARD);
    assertFalse(run.isRunning());
  }

  @Test
  void anInterruptWaitsForTheCurrentStepThenFollowsTheNewOrder() {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    run.tick(DOOR);
    run.order(DOOR, TimelineOrder.BACKWARD);
    assertEquals(TimelineOrder.BACKWARD, run.pending());
    for (int tick = 1; tick < WAIT - 1; tick++) {
      run.tick(DOOR);
      assertEquals(1, run.outputs(DOOR));
    }
    run.tick(DOOR);
    assertEquals(0, run.outputs(DOOR));
    assertEquals(TimelineOrder.BACKWARD, run.travel());
    assertNull(run.pending());
  }

  @ParameterizedTest(name = "interrupted after {0} ticks")
  @CsvSource({"0", "1", "3", "4", "5", "7"})
  void anInterruptedRunAlwaysEndsWithEveryOutputOff(int ticks) {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    for (int tick = 0; tick < ticks; tick++) {
      run.tick(DOOR);
    }
    run.order(DOOR, TimelineOrder.BACKWARD);
    outputsOver(run, DOOR, 30);
    assertEquals(0, run.outputs(DOOR));
    assertFalse(run.isRunning());
  }

  @Test
  void theLastOrderWinsWhileAStepRuns() {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    run.order(DOOR, TimelineOrder.BACKWARD);
    run.order(DOOR, TimelineOrder.FORWARD);
    assertNull(run.pending());
    outputsOver(run, DOOR, 20);
    assertEquals(2, run.position());
  }

  @Test
  void stopFinishesTheStepAndHoldsWhereItIs() {
    TimelineRun run = new TimelineRun();
    run.order(DOOR, TimelineOrder.FORWARD);
    run.order(DOOR, TimelineOrder.STOP);
    assertEquals(List.of(1), outputsOver(run, DOOR, 20));
    assertFalse(run.isRunning());
    assertEquals(1, run.position());
  }

  @Test
  void autoReverseStartsBackwardsOnceItsTimeIsUp() {
    TimelineRun run = finishedForward(SELF_CLOSING);
    assertTrue(run.isRunning());
    assertEquals(10, run.reverseLeft());
    assertEquals(List.of(3, 1, 0), outputsOver(run, SELF_CLOSING, 30));
    assertFalse(run.isRunning());
  }

  private static Stream<Arguments> ordersDuringHold() {
    return Stream.of(
        Arguments.of(TimelineOrder.FORWARD, 10, 2, null),
        Arguments.of(TimelineOrder.BACKWARD, 0, 1, TimelineOrder.BACKWARD),
        Arguments.of(TimelineOrder.STOP, 0, 2, null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("ordersDuringHold")
  void ordersDuringAnAutoReverseHold(
      TimelineOrder order, int reverseLeft, int position, TimelineOrder travel) {
    TimelineRun run = finishedForward(SELF_CLOSING);
    run.tick(SELF_CLOSING);
    run.order(SELF_CLOSING, order);
    assertEquals(reverseLeft, run.reverseLeft());
    assertEquals(position, run.position());
    assertEquals(travel, run.travel());
  }

  private static Stream<Arguments> toggles() {
    return Stream.of(
        Arguments.of(0, null, null, TimelineOrder.FORWARD),
        Arguments.of(1, null, null, TimelineOrder.FORWARD),
        Arguments.of(2, null, null, TimelineOrder.BACKWARD),
        Arguments.of(1, TimelineOrder.FORWARD, null, TimelineOrder.STOP),
        Arguments.of(1, TimelineOrder.BACKWARD, null, TimelineOrder.STOP),
        Arguments.of(1, TimelineOrder.FORWARD, TimelineOrder.STOP, TimelineOrder.FORWARD),
        Arguments.of(1, TimelineOrder.BACKWARD, TimelineOrder.STOP, TimelineOrder.BACKWARD));
  }

  @ParameterizedTest(name = "at {0}, {1}, pending {2}: {3}")
  @MethodSource("toggles")
  void aToggleRunsOrStops(
      int position, TimelineOrder travel, TimelineOrder pending, TimelineOrder expected) {
    TimelineRun run = TimelineRun.restored(DOOR, position, travel, 3, pending, 0);
    assertEquals(expected, run.toggleOrder(DOOR));
  }

  private static Stream<Arguments> shorterTimelines() {
    Timeline one = new Timeline(List.of(new TimelineStep(1, WAIT)), 0);
    return Stream.of(Arguments.of(one, 1, true), Arguments.of(Timeline.EMPTY, 0, false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("shorterTimelines")
  void aShorterTimelineClampsThePlace(Timeline shorter, int position, boolean running) {
    TimelineRun run = TimelineRun.restored(DOOR, 2, TimelineOrder.FORWARD, 3, null, 0);
    run.retarget(shorter);
    assertEquals(position, run.position());
    assertEquals(running, run.isRunning());
    outputsOver(run, shorter, 20);
    assertFalse(run.isRunning());
  }

  @Test
  void aNewTimelineWithoutAutoReverseCancelsTheHold() {
    TimelineRun run = finishedForward(SELF_CLOSING);
    run.retarget(DOOR);
    assertFalse(run.isRunning());
  }

  private static Stream<Arguments> badPlaces() {
    return Stream.of(
        Arguments.of(99, null, 0, null, 0, 2, false),
        Arguments.of(-5, null, 0, null, 0, 0, false),
        Arguments.of(1, TimelineOrder.STOP, 5, null, 0, 1, false),
        Arguments.of(1, null, 0, TimelineOrder.FORWARD, 0, 1, false),
        Arguments.of(1, null, 0, null, 50, 1, false));
  }

  @ParameterizedTest(name = "position {0}, travel {1}")
  @MethodSource("badPlaces")
  void aDamagedSaveComesBackAsAPlaceTheTimelineHas(
      int position,
      TimelineOrder travel,
      int ticksLeft,
      TimelineOrder pending,
      int reverseLeft,
      int expectedPosition,
      boolean running) {
    TimelineRun run = TimelineRun.restored(DOOR, position, travel, ticksLeft, pending, reverseLeft);
    assertEquals(expectedPosition, run.position());
    assertEquals(running, run.isRunning());
  }
}
