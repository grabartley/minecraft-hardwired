package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.device.DeviceStatus;

public enum GridState {
  NO_SOURCE(DeviceStatus.NO_SOURCE),
  SOURCES_OFF(DeviceStatus.SOURCES_OFF),
  OUT_OF_FUEL(DeviceStatus.OUT_OF_FUEL),
  OVERLOADED(DeviceStatus.OVERLOADED),
  RETURNING(DeviceStatus.POWER_RETURNING),
  LIVE(DeviceStatus.RUNNING);

  private static final GridState[] ALL = values();

  private final DeviceStatus stopReason;

  GridState(DeviceStatus stopReason) {
    this.stopReason = stopReason;
  }

  public DeviceStatus stopReason() {
    return stopReason;
  }

  public boolean givesPower() {
    return this == LIVE;
  }

  public static GridState byIndex(int index) {
    return ALL[Math.clamp(index, 0, ALL.length - 1)];
  }
}
