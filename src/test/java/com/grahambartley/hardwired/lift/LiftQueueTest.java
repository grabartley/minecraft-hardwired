package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LiftQueueTest {

  static Stream<Arguments> calls() {
    return Stream.of(
        Arguments.of(new int[] {5, 2, 9}, new int[] {5, 2, 9}),
        Arguments.of(new int[] {5, 5, 5}, new int[] {5}),
        Arguments.of(new int[] {5, 2, 5, 2}, new int[] {5, 2}),
        Arguments.of(new int[] {}, new int[] {}));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("calls")
  void callsAreAnsweredInOrderOnceEach(int[] calls, int[] served) {
    LiftQueue queue = new LiftQueue();
    Arrays.stream(calls).forEach(queue::add);
    assertArrayEquals(served, queue.toArray());
  }

  @Test
  void aSavedQueueLoadsBack() {
    LiftQueue queue = new LiftQueue();
    queue.load(new int[] {3, 1});
    assertEquals(3, queue.poll());
    assertEquals(1, queue.poll());
    assertTrue(queue.isEmpty());
  }

  @Test
  void aFullQueueRefusesMoreCalls() {
    LiftQueue queue = new LiftQueue();
    for (int level = 0; level < LiftQueue.MAX_CALLS; level++) {
      assertTrue(queue.add(level));
    }
    assertFalse(queue.add(LiftQueue.MAX_CALLS));
  }
}
