package com.grahambartley.hardwired.circuit;

public record Part(PartType type, int x, int y, int setting) {

  public static Part of(PartType type, int x, int y) {
    return new Part(type, x, y, type.defaultSetting());
  }

  public Part movedTo(int newX, int newY) {
    return new Part(type, newX, newY, setting);
  }

  public Part withSetting(int value) {
    return new Part(type, x, y, value);
  }
}
