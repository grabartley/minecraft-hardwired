package com.grahambartley.hardwired.circuit;

import java.util.Locale;

public enum Refusal {
  BAD_TEXT,
  NEWER_VERSION,
  TOO_LONG,
  TOO_MANY_PARTS,
  OFF_THE_GRID,
  PARTS_OVERLAP,
  BAD_SETTING,
  BAD_LINE,
  INPUT_TAKEN,
  LOOP_WITHOUT_DELAY,
  EMPTY,
  NEEDS_BLANK,
  NEEDS_REDSTONE,
  NOT_HOLDING_CIRCUIT,
  TOO_FAST;

  public String key() {
    return "screen.hardwired.circuit.refused." + name().toLowerCase(Locale.ROOT);
  }
}
