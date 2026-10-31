package com.grahambartley.hardwired.lift;

public enum LiftPhase {
  RESTING,
  MOVING,
  STOPPED,
  LOWERING;

  public boolean travelling() {
    return this == MOVING || this == LOWERING;
  }

  public static LiftPhase byName(String name) {
    for (LiftPhase phase : values()) {
      if (phase.name().equals(name)) {
        return phase;
      }
    }
    return RESTING;
  }
}
