package com.grahambartley.hardwired.device;

public final class ElectronicsRule {

  private ElectronicsRule() {}

  public static DeviceStatus next(DeviceStatus demand, boolean powered, DeviceStatus stopReason) {
    return demand != DeviceStatus.RUNNING || powered ? demand : stopReason;
  }

  public static boolean beatDue(long beatAt, long now) {
    return now - beatAt >= ElectronicsDriver.BEAT_TICKS;
  }
}
