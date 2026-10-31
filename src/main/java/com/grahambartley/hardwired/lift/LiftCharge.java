package com.grahambartley.hardwired.lift;

import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsRule;

public final class LiftCharge {

  private LiftCharge() {}

  public static boolean owes(int blocksStarted, int blocksPaid) {
    return blocksStarted > blocksPaid;
  }

  public static DeviceStatus stopReason(boolean hasCell) {
    return ElectronicsRule.next(DeviceStatus.RUNNING, false, hasCell);
  }

  public static DeviceStatus demand(
      LiftPhase phase, DeviceStatus stopReason, int chargePerBlock, boolean hasCell, int stored) {
    return switch (phase) {
      case MOVING -> DeviceStatus.RUNNING;
      case STOPPED -> stopReason;
      case RESTING, LOWERING -> ElectronicsRule.readiness(chargePerBlock, hasCell, stored);
    };
  }
}
