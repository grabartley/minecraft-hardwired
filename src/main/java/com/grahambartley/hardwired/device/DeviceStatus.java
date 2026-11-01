package com.grahambartley.hardwired.device;

public enum DeviceStatus {
  RUNNING("running"),
  READY("ready"),
  WIRE_OFF("wire_off"),
  NO_SOURCE("no_source"),
  SOURCES_OFF("sources_off"),
  OUT_OF_FUEL("out_of_fuel"),
  OVERLOADED("overloaded"),
  POWER_RETURNING("power_returning"),
  NO_RADIO("no_radio"),
  RADIO_OFF("radio_off"),
  NO_STATION("no_station"),
  NO_TOWER("no_tower"),
  TOWER_TOO_SHORT("tower_too_short"),
  TOWER_NO_ROD("tower_no_rod"),
  TOWER_IN_USE("tower_in_use"),
  SHAFT_BLOCKED("shaft_blocked"),
  NO_LIFT("no_lift");

  private final String id;

  DeviceStatus(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public String translationKey() {
    return "device.hardwired.status." + id;
  }

  public boolean isStopped() {
    return this != RUNNING && this != READY;
  }

  public boolean wantsPower() {
    return this == RUNNING || isPowerStop();
  }

  public boolean isPowerStop() {
    return this == NO_SOURCE
        || this == SOURCES_OFF
        || this == OUT_OF_FUEL
        || this == OVERLOADED
        || this == POWER_RETURNING;
  }

  public static DeviceStatus byId(String id, DeviceStatus fallback) {
    for (DeviceStatus status : values()) {
      if (status.id.equals(id)) {
        return status;
      }
    }
    return fallback;
  }
}
