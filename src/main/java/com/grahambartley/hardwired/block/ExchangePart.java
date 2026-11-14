package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum ExchangePart implements StringIdentifiable {
  LOWER_LEFT("lower_left", false, false),
  LOWER_RIGHT("lower_right", true, false),
  UPPER_LEFT("upper_left", false, true),
  UPPER_RIGHT("upper_right", true, true);

  private final String id;
  private final boolean right;
  private final boolean upper;

  ExchangePart(String id, boolean right, boolean upper) {
    this.id = id;
    this.right = right;
    this.upper = upper;
  }

  public boolean right() {
    return right;
  }

  public boolean upper() {
    return upper;
  }

  @Override
  public String asString() {
    return id;
  }
}
