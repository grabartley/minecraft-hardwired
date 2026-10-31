package com.grahambartley.hardwired.circuit;

public enum Side {
  NORTH,
  EAST,
  SOUTH,
  WEST;

  public static final int COUNT = 4;

  private static final Side[] ALL = values();

  public static Side byIndex(int index) {
    return ALL[Math.floorMod(index, COUNT)];
  }

  public Side turned(int quarterTurns) {
    return byIndex(ordinal() + quarterTurns);
  }

  public int bit() {
    return 1 << ordinal();
  }
}
