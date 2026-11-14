package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum PhoneBoxPart implements StringIdentifiable {
  LOWER("lower", 0),
  MIDDLE("middle", 1),
  UPPER("upper", 2);

  private final String id;
  private final int height;

  PhoneBoxPart(String id, int height) {
    this.id = id;
    this.height = height;
  }

  public int height() {
    return height;
  }

  @Override
  public String asString() {
    return id;
  }
}
