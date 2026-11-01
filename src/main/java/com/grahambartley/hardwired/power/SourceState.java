package com.grahambartley.hardwired.power;

public enum SourceState {
  SWITCHED_OFF,
  NO_FUEL,
  READY;

  private static final SourceState[] ALL = values();

  public static SourceState byIndex(int index) {
    return ALL[Math.clamp(index, 0, ALL.length - 1)];
  }

  public static SourceState of(boolean switchClosed, boolean fuelled) {
    if (!switchClosed) {
      return SWITCHED_OFF;
    }
    return fuelled ? READY : NO_FUEL;
  }
}
