package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum PoleWood implements StringIdentifiable {
  OAK("oak"),
  SPRUCE("spruce"),
  DARK_OAK("dark_oak");

  private final String id;

  PoleWood(String id) {
    this.id = id;
  }

  @Override
  public String asString() {
    return id;
  }
}
