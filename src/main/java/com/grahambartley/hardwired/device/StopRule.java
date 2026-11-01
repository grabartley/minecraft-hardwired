package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.power.SourceState;

public final class StopRule {

  private StopRule() {}

  public static DeviceStatus ofDevice(boolean powered, boolean on, DeviceStatus gridReason) {
    if (!powered) {
      return gridReason;
    }
    return on ? DeviceStatus.RUNNING : DeviceStatus.WIRE_OFF;
  }

  public static DeviceStatus ofSource(SourceState own, boolean wired, DeviceStatus gridReason) {
    return switch (own) {
      case SWITCHED_OFF -> DeviceStatus.SOURCES_OFF;
      case NO_FUEL -> DeviceStatus.OUT_OF_FUEL;
      case READY, READY_FREE -> wired ? gridReason : DeviceStatus.READY;
    };
  }
}
