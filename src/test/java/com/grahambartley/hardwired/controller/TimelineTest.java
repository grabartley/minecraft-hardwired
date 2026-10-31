package com.grahambartley.hardwired.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimelineTest {

  private static final Timeline TWO =
      new Timeline(List.of(new TimelineStep(1, 4), new TimelineStep(3, 6)), 0);

  @ParameterizedTest(name = "position {0}: outputs {1}")
  @CsvSource({"-1, 0", "0, 0", "1, 1", "2, 3", "9, 3"})
  void outputsFollowThePlace(int position, int outputs) {
    assertEquals(outputs, TWO.outputsAt(position));
  }

  @ParameterizedTest(name = "position {0}: wait {1}")
  @CsvSource({"1, 4", "2, 6"})
  void waitBelongsToTheStepBehindThePlace(int position, int wait) {
    assertEquals(wait, TWO.waitBefore(position));
  }

  @Test
  void aTimelineNeverHoldsMoreThanTheStepLimit() {
    List<TimelineStep> many = Collections.nCopies(Timeline.MAX_STEPS + 40, new TimelineStep(1, 4));
    assertEquals(Timeline.MAX_STEPS, new Timeline(many, 0).size());
  }

  @ParameterizedTest(name = "{0} becomes {1}")
  @CsvSource({"-1, 0", "0, 0", "100, 100", "99999, 6000"})
  void autoReverseStaysInBounds(int ticks, int expected) {
    assertEquals(expected, new Timeline(List.of(), ticks).autoReverseTicks());
  }
}
