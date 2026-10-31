package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.wire.Hue;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ControllerDraftTest {

  private static ControllerDraft blank() {
    return new ControllerDraft(ControllerProgram.DEFAULT);
  }

  @Test
  void anUntouchedDraftEqualsTheProgram() {
    assertEquals(ControllerProgram.DEFAULT, blank().program());
  }

  @Test
  void aNewStepStartsFromTheLastOutputs() {
    ControllerDraft draft = blank();
    draft.addStep();
    draft.toggle(0, 2);
    draft.addStep();
    assertTrue(draft.steps().get(1).isOn(2));
  }

  @Test
  void stepsStopAtTheLimit() {
    ControllerDraft draft = blank();
    for (int step = 0; step < Timeline.MAX_STEPS; step++) {
      assertTrue(draft.addStep());
    }
    assertFalse(draft.addStep());
  }

  @ParameterizedTest(name = "remove {0}: {1} left")
  @CsvSource({"-1, 2", "0, 1", "1, 1", "2, 2"})
  void removingIgnoresAMissingStep(int step, int left) {
    ControllerDraft draft = blank();
    draft.addStep();
    draft.addStep();
    draft.removeStep(step);
    assertEquals(left, draft.steps().size());
  }

  @ParameterizedTest(name = "wait change {0}")
  @CsvSource({"-999, 2", "2, 12", "9999, 1200"})
  void waitsStayInBounds(int delta, int expected) {
    ControllerDraft draft = blank();
    draft.addStep();
    draft.adjustWait(0, delta);
    assertEquals(expected, draft.steps().get(0).waitTicks());
  }

  @ParameterizedTest(name = "auto reverse change {0}")
  @CsvSource({"-20, 0", "20, 20", "99999, 6000"})
  void autoReverseStaysInBounds(int delta, int expected) {
    ControllerDraft draft = blank();
    draft.adjustAutoReverse(delta);
    assertEquals(expected, draft.autoReverseTicks());
  }

  @ParameterizedTest(name = "direction {0}")
  @ValueSource(ints = {1, -1})
  void outputCyclingSkipsColoursAlreadyTaken(int direction) {
    ControllerDraft draft = blank();
    Set<Optional<Hue>> seen = new HashSet<>();
    for (int turn = 0; turn < Hue.values().length; turn++) {
      draft.cycleOutput(5, direction);
      seen.add(draft.output(5));
    }
    assertFalse(seen.contains(Optional.of(Hue.PLAIN)));
    assertFalse(seen.contains(Optional.of(Hue.RED)));
    assertTrue(seen.contains(Optional.empty()));
  }

  @Test
  void readingAnOutputColourFreesItsSlot() {
    ControllerDraft draft = blank();
    while (draft.open() != Hue.RED) {
      draft.cycleOpen(1);
    }
    assertEquals(Optional.empty(), draft.output(0));
    assertEquals(draft.program().wiring().open(), Hue.RED);
  }

  @Test
  void togglesReachTheProgram() {
    ControllerDraft draft = blank();
    draft.addStep();
    draft.toggle(0, 1);
    assertEquals(
        new TimelineStep(2, TimelineStep.DEFAULT_WAIT_TICKS),
        draft.program().timeline().steps().get(0));
  }
}
