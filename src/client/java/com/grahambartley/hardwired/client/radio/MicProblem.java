package com.grahambartley.hardwired.client.radio;

public enum MicProblem {
  NO_INPUT("mic_missing"),
  BUSY("mic_busy"),
  REFUSED("mic_refused");

  private final String key;

  MicProblem(String key) {
    this.key = key;
  }

  public String translationKey() {
    return "screen.hardwired.desk." + key;
  }
}
