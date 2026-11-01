package com.grahambartley.hardwired.device;

import java.util.List;
import java.util.function.Supplier;

public final class ElectronicsRule {

  private ElectronicsRule() {}

  public static DeviceStatus next(DeviceStatus demand, boolean powered, DeviceStatus stopReason) {
    return demand != DeviceStatus.RUNNING || powered ? demand : stopReason;
  }

  public static boolean beatDue(long beatAt, long now) {
    return now - beatAt >= ElectronicsDriver.BEAT_TICKS;
  }

  public static boolean lightsOut(
      DeviceStatus before, DeviceStatus after, Supplier<List<DeviceStatus>> network) {
    return before == DeviceStatus.RUNNING
        && after.isPowerStop()
        && network.get().stream().noneMatch(status -> status == DeviceStatus.RUNNING);
  }
}
