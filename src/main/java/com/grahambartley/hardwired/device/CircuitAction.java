package com.grahambartley.hardwired.device;

public enum CircuitAction {
  SAVE,
  PASTE,
  LOAD,
  MAKE;

  public static CircuitAction byIndex(int index) {
    CircuitAction[] all = values();
    return index >= 0 && index < all.length ? all[index] : null;
  }
}
