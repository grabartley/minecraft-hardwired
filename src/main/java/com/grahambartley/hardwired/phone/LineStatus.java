package com.grahambartley.hardwired.phone;

public enum LineStatus {
  DIAL_TONE("screen.hardwired.telephone.status.dial_tone"),
  RINGING_OUT("screen.hardwired.telephone.status.ringing_out"),
  CONNECTED("screen.hardwired.telephone.status.connected"),
  BUSY("screen.hardwired.telephone.status.busy"),
  NO_SUCH_NUMBER("screen.hardwired.telephone.status.no_such_number"),
  NO_ANSWER("screen.hardwired.telephone.status.no_answer"),
  HUNG_UP("screen.hardwired.telephone.status.hung_up"),
  LINE_DEAD("screen.hardwired.telephone.status.line_dead"),
  TOO_FAST("device.hardwired.too_fast");

  private final String key;

  LineStatus(String key) {
    this.key = key;
  }

  public String translationKey() {
    return key;
  }

  public boolean isOnCall() {
    return this == RINGING_OUT || this == CONNECTED;
  }

  public static LineStatus byIndex(int index) {
    LineStatus[] values = values();
    return values[Math.floorMod(index, values.length)];
  }
}
