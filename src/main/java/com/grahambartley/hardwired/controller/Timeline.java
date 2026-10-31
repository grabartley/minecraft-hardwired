package com.grahambartley.hardwired.controller;

import java.util.List;

public record Timeline(List<TimelineStep> steps, int autoReverseTicks) {

  public static final int MAX_STEPS = 32;
  public static final int MAX_AUTO_REVERSE_TICKS = 6000;
  public static final Timeline EMPTY = new Timeline(List.of(), 0);

  public Timeline {
    steps = List.copyOf(steps.subList(0, Math.min(steps.size(), MAX_STEPS)));
    autoReverseTicks = Math.clamp(autoReverseTicks, 0, MAX_AUTO_REVERSE_TICKS);
  }

  public int size() {
    return steps.size();
  }

  public int outputsAt(int position) {
    return position <= 0 ? 0 : steps.get(Math.min(position, size()) - 1).outputs();
  }

  public int waitBefore(int position) {
    return steps.get(position - 1).waitTicks();
  }
}
