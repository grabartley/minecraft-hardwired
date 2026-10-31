package com.grahambartley.hardwired.controller;

public final class TimelineInputs {

  private TimelineInputs() {}

  public static TimelineOrder orderFor(
      boolean oneColour, boolean wasOpen, boolean wasClose, boolean open, boolean close) {
    if (oneColour) {
      if (open == wasOpen) {
        return null;
      }
      return open ? TimelineOrder.FORWARD : TimelineOrder.BACKWARD;
    }
    if (close && !wasClose) {
      return TimelineOrder.BACKWARD;
    }
    return open && !wasOpen ? TimelineOrder.FORWARD : null;
  }
}
