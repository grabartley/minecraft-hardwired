package com.grahambartley.hardwired.device;

public enum PanelAction {
  REFRESH,
  TOGGLE,
  SET_LOOK;

  public static PanelAction byIndex(int index) {
    return values()[Math.floorMod(index, values().length)];
  }
}
