package com.grahambartley.hardwired.controller;

public record TimelineStep(int outputs, int waitTicks) {

  public static final int OUTPUT_SLOTS = 8;
  public static final int MIN_WAIT_TICKS = 2;
  public static final int MAX_WAIT_TICKS = 1200;
  public static final int DEFAULT_WAIT_TICKS = 10;
  private static final int ALL_OUTPUTS = (1 << OUTPUT_SLOTS) - 1;

  public TimelineStep {
    outputs &= ALL_OUTPUTS;
    waitTicks = Math.clamp(waitTicks, MIN_WAIT_TICKS, MAX_WAIT_TICKS);
  }

  public boolean isOn(int slot) {
    return (outputs & (1 << slot)) != 0;
  }

  public TimelineStep toggled(int slot) {
    return new TimelineStep(outputs ^ (1 << slot), waitTicks);
  }

  public TimelineStep withWait(int ticks) {
    return new TimelineStep(outputs, ticks);
  }
}
