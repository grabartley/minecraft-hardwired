package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum PhoneBoxPart implements StringIdentifiable {
  LOWER("lower"),
  MIDDLE("middle"),
  UPPER("upper");

  private final String id;

  PhoneBoxPart(String id) {
    this.id = id;
  }

  public int height() {
    return ordinal();
  }

  @Override
  public String asString() {
    return id;
  }
}
