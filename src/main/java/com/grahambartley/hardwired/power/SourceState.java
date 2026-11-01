package com.grahambartley.hardwired.power;

public enum SourceState {
  SWITCHED_OFF,
  NO_FUEL,
  READY,
  READY_FREE;

  private static final SourceState[] ALL = values();

  public static SourceState byIndex(int index) {
    return ALL[Math.clamp(index, 0, ALL.length - 1)];
  }

  public boolean isReady() {
    return this == READY || this == READY_FREE;
  }

  public static SourceState of(boolean switchClosed, boolean fuelled, boolean burnsFuel) {
    if (!switchClosed) {
      return SWITCHED_OFF;
    }
    if (!fuelled) {
      return NO_FUEL;
    }
    return burnsFuel ? READY : READY_FREE;
  }
}
