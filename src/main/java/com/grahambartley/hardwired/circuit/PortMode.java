package com.grahambartley.hardwired.circuit;

public enum PortMode {
  UNUSED('u'),
  INPUT('i'),
  OUTPUT('o');

  private final char code;

  PortMode(char code) {
    this.code = code;
  }

  public char code() {
    return code;
  }

  public static PortMode byCode(char code) {
    for (PortMode mode : values()) {
      if (mode.code == code) {
        return mode;
      }
    }
    return null;
  }

  public PortMode next() {
    return values()[(ordinal() + 1) % values().length];
  }
}
