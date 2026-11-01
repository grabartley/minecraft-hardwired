package com.grahambartley.hardwired.block;

import net.minecraft.util.StringIdentifiable;

public enum DeskPart implements StringIdentifiable {
  PANEL("panel"),
  TOP("top"),
  DESK("desk");

  private final String name;

  DeskPart(String name) {
    this.name = name;
  }

  @Override
  public String asString() {
    return name;
  }
}
