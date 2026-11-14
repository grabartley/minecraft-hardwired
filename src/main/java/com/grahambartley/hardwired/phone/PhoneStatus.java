package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.device.DeviceStatus;

public final class PhoneStatus {

  private PhoneStatus() {}

  public static DeviceStatus of(
      boolean listed, boolean exchange, boolean onSystem, boolean numbered, boolean allowed) {
    if (!listed) {
      return DeviceStatus.NO_EXCHANGE;
    }
    if (!onSystem) {
      return exchange ? DeviceStatus.NO_TELEPHONE : DeviceStatus.NO_EXCHANGE;
    }
    if (numbered) {
      return DeviceStatus.READY;
    }
    return allowed ? DeviceStatus.NETWORK_FULL : DeviceStatus.SEALED_OUT;
  }
}
