package com.grahambartley.hardwired.circuit;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Validator {

  private static final int PINS_PER_PART = 2;

  private Validator() {}

  public static Optional<Refusal> check(Design design, int maxParts) {
    if (design.sides().size() != Side.COUNT) {
      return Optional.of(Refusal.BAD_TEXT);
    }
    if (design.parts().size() > maxParts) {
      return Optional.of(Refusal.TOO_MANY_PARTS);
    }
    return checkParts(design.parts())
        .or(() -> checkLines(design))
        .or(
            () ->
                LoopCheck.hasLoop(design)
                    ? Optional.of(Refusal.LOOP_WITHOUT_DELAY)
                    : Optional.empty());
  }

  private static Optional<Refusal> checkParts(List<Part> parts) {
    Set<Integer> taken = new HashSet<>();
    for (Part part : parts) {
      if (part.x() < 0
          || part.y() < 0
          || part.x() >= Design.GRID_WIDTH
          || part.y() >= Design.GRID_HEIGHT) {
        return Optional.of(Refusal.OFF_THE_GRID);
      }
      if (!taken.add(part.y() * Design.GRID_WIDTH + part.x())) {
        return Optional.of(Refusal.PARTS_OVERLAP);
      }
      if (part.setting() < part.type().minSetting() || part.setting() > part.type().maxSetting()) {
        return Optional.of(Refusal.BAD_SETTING);
      }
    }
    return Optional.empty();
  }

  private static Optional<Refusal> checkLines(Design design) {
    if (design.lines().size() > Side.COUNT + PINS_PER_PART * design.parts().size()) {
      return Optional.of(Refusal.INPUT_TAKEN);
    }
    Set<Long> sinks = new HashSet<>();
    for (Line line : design.lines()) {
      if (!isSource(design, line.from()) || !isSink(design, line.to(), line.pin())) {
        return Optional.of(Refusal.BAD_LINE);
      }
      if (!sinks.add((long) line.to() << Integer.SIZE | line.pin())) {
        return Optional.of(Refusal.INPUT_TAKEN);
      }
    }
    return Optional.empty();
  }

  private static boolean isSource(Design design, int node) {
    if (node < 0 || node >= design.nodes()) {
      return false;
    }
    return !design.isSide(node) || design.mode(Side.byIndex(node)) == PortMode.INPUT;
  }

  private static boolean isSink(Design design, int node, int pin) {
    if (node < 0 || node >= design.nodes() || pin < 0) {
      return false;
    }
    if (design.isSide(node)) {
      return pin == 0 && design.mode(Side.byIndex(node)) == PortMode.OUTPUT;
    }
    return pin < design.partAt(node).type().inputs();
  }
}
