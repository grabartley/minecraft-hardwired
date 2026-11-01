package com.grahambartley.hardwired.radio;

public enum DeskAction {
  SET_FREQUENCY,
  TOGGLE_AIR,
  PLAY,
  STOP,
  EJECT;

  public static DeskAction byIndex(int index) {
    return values()[Math.floorMod(index, values().length)];
  }
}
