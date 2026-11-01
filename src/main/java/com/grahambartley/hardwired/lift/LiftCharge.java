package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.device.DeviceStatus;

public final class LiftCharge {

  private LiftCharge() {}

  public static boolean owes(int blocksStarted, int blocksPaid) {
    return blocksStarted > blocksPaid;
  }

  public static DeviceStatus demand(LiftPhase phase, DeviceStatus stopReason, DeviceStatus power) {
    return switch (phase) {
      case MOVING -> DeviceStatus.RUNNING;
      case STOPPED -> stopReason;
      case RESTING, LOWERING -> power;
    };
  }
}
