package com.grahambartley.hardwired.device;

import java.util.List;
import java.util.function.Supplier;

public final class ElectronicsRule {

  private ElectronicsRule() {}

  public static DeviceStatus next(DeviceStatus demand, boolean paid, boolean hasCell) {
    if (demand != DeviceStatus.RUNNING || paid) {
      return demand;
    }
    return hasCell ? DeviceStatus.NO_CHARGE : DeviceStatus.NO_CELL;
  }

  public static boolean beatPaid(long paidAt, long now) {
    return now - paidAt < ElectronicsDriver.BEAT_TICKS;
  }

  public static DeviceStatus readiness(int cost, boolean hasCell, int available) {
    if (available >= cost) {
      return DeviceStatus.READY;
    }
    return hasCell ? DeviceStatus.NO_CHARGE : DeviceStatus.NO_CELL;
  }

  public static boolean lightsOut(
      DeviceStatus before, DeviceStatus after, Supplier<List<DeviceStatus>> network) {
    return before == DeviceStatus.RUNNING
        && after == DeviceStatus.NO_CHARGE
        && network.get().stream().noneMatch(status -> status == DeviceStatus.RUNNING);
  }
}
