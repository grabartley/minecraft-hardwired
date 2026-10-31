package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.controller.ControllerProgram;
import com.grahambartley.hardwired.controller.ControllerWiring;
import com.grahambartley.hardwired.controller.Timeline;
import com.grahambartley.hardwired.controller.TimelineStep;
import com.grahambartley.hardwired.wire.Hue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ControllerDraft {

  private static final int NO_HUE = Hue.values().length;
  private static final int CHOICES = NO_HUE + 1;

  private Hue open;
  private Hue close;
  private final List<Optional<Hue>> outputs;
  private final List<TimelineStep> steps;
  private int autoReverseTicks;

  public ControllerDraft(ControllerProgram program) {
    open = program.wiring().open();
    close = program.wiring().close();
    outputs = new ArrayList<>(program.wiring().outputs());
    steps = new ArrayList<>(program.timeline().steps());
    autoReverseTicks = program.timeline().autoReverseTicks();
  }

  public Hue open() {
    return open;
  }

  public Hue close() {
    return close;
  }

  public Optional<Hue> output(int slot) {
    return outputs.get(slot);
  }

  public List<TimelineStep> steps() {
    return steps;
  }

  public int autoReverseTicks() {
    return autoReverseTicks;
  }

  public void cycleOpen(int delta) {
    open = Hue.byIndex(open.ordinal() + delta);
    clearOutputsReading();
  }

  public void cycleClose(int delta) {
    close = Hue.byIndex(close.ordinal() + delta);
    clearOutputsReading();
  }

  private void clearOutputsReading() {
    outputs.replaceAll(hue -> hue.filter(color -> color != open && color != close));
  }

  public void cycleOutput(int slot, int delta) {
    int choice = choiceOf(outputs.get(slot));
    for (int tried = 0; tried < CHOICES; tried++) {
      choice = Math.floorMod(choice + delta, CHOICES);
      Optional<Hue> next = hueOf(choice);
      if (isFree(next)) {
        outputs.set(slot, next);
        return;
      }
    }
  }

  private boolean isFree(Optional<Hue> hue) {
    return hue.isEmpty() || (hue.get() != open && hue.get() != close && !outputs.contains(hue));
  }

  private static int choiceOf(Optional<Hue> hue) {
    return hue.map(Hue::ordinal).orElse(NO_HUE);
  }

  private static Optional<Hue> hueOf(int choice) {
    return choice == NO_HUE ? Optional.empty() : Optional.of(Hue.byIndex(choice));
  }

  public void toggle(int step, int slot) {
    steps.set(step, steps.get(step).toggled(slot));
  }

  public boolean addStep() {
    if (steps.size() >= Timeline.MAX_STEPS) {
      return false;
    }
    int outputsNow = steps.isEmpty() ? 0 : steps.get(steps.size() - 1).outputs();
    steps.add(new TimelineStep(outputsNow, TimelineStep.DEFAULT_WAIT_TICKS));
    return true;
  }

  public void removeStep(int step) {
    if (step >= 0 && step < steps.size()) {
      steps.remove(step);
    }
  }

  public void adjustWait(int step, int deltaTicks) {
    if (step >= 0 && step < steps.size()) {
      TimelineStep current = steps.get(step);
      steps.set(step, current.withWait(current.waitTicks() + deltaTicks));
    }
  }

  public void adjustAutoReverse(int deltaTicks) {
    autoReverseTicks =
        Math.clamp(autoReverseTicks + deltaTicks, 0, Timeline.MAX_AUTO_REVERSE_TICKS);
  }

  public ControllerProgram program() {
    return new ControllerProgram(
        new ControllerWiring(open, close, outputs), new Timeline(steps, autoReverseTicks));
  }
}
