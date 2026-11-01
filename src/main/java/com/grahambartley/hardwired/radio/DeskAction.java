package com.grahambartley.hardwired.radio;

public enum DeskAction {
  SET_FREQUENCY,
  TOGGLE_AIR,
  PLAY,
  STOP,
  EJECT,
  TAKE_MIC,
  RELEASE_MIC,
  RESTART;

  public static DeskAction byIndex(int index) {
    DeskAction[] all = values();
    return index >= 0 && index < all.length ? all[index] : null;
  }
}
