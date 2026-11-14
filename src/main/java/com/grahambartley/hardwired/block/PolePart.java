package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum PolePart implements StringIdentifiable {
  SINGLE("single"),
  FOOT("foot"),
  MIDDLE("middle"),
  TOP("top");

  private final String id;

  PolePart(String id) {
    this.id = id;
  }

  public static PolePart between(boolean poleBelow, boolean poleAbove) {
    if (poleBelow) {
      return poleAbove ? MIDDLE : TOP;
    }
    return poleAbove ? FOOT : SINGLE;
  }

  public boolean holdsSpans() {
    return this == TOP || this == SINGLE;
  }

  @Override
  public String asString() {
    return id;
  }
}
