package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.controller.TimelineStep;
import java.util.Optional;
import java.util.OptionalInt;

public record TimelineGrid(int x, int y, int cell) {

  public static final int VISIBLE_STEPS = 16;

  public record Cell(int step, int slot) {}

  public int columnX(int step, int firstStep) {
    return x + (step - firstStep) * cell;
  }

  public int rowY(int slot) {
    return y + slot * cell;
  }

  public int headerY() {
    return y - cell;
  }

  public int labelX() {
    return x - cell - 2;
  }

  public Optional<Cell> cellAt(double mouseX, double mouseY, int firstStep, int steps) {
    OptionalInt step = stepAt(mouseX, firstStep, steps);
    int slot = (int) Math.floor((mouseY - y) / cell);
    if (step.isEmpty() || slot < 0 || slot >= TimelineStep.OUTPUT_SLOTS) {
      return Optional.empty();
    }
    return Optional.of(new Cell(step.getAsInt(), slot));
  }

  public OptionalInt headerAt(double mouseX, double mouseY, int firstStep, int steps) {
    return mouseY >= headerY() && mouseY < y
        ? stepAt(mouseX, firstStep, steps)
        : OptionalInt.empty();
  }

  public OptionalInt labelAt(double mouseX, double mouseY) {
    int slot = (int) Math.floor((mouseY - y) / cell);
    boolean inColumn = mouseX >= labelX() && mouseX < labelX() + cell;
    return inColumn && slot >= 0 && slot < TimelineStep.OUTPUT_SLOTS
        ? OptionalInt.of(slot)
        : OptionalInt.empty();
  }

  private OptionalInt stepAt(double mouseX, int firstStep, int steps) {
    int step = firstStep + (int) Math.floor((mouseX - x) / cell);
    boolean visible = step >= firstStep && step < firstStep + VISIBLE_STEPS;
    return visible && step < steps ? OptionalInt.of(step) : OptionalInt.empty();
  }

  public static int pageStart(int step) {
    return Math.max(0, step) / VISIBLE_STEPS * VISIBLE_STEPS;
  }
}
