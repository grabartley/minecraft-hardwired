package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.Optional;

public final class DeskRun {

  public static final long NOT_ON_AIR = -1;
  private static final long LONGEST_LIVE_GAP = 60;

  private DeskRun() {}

  public static DeviceStatus demand(
      boolean wantsAir, Optional<TowerColumn> tower, boolean inUse, RadioSettings settings) {
    if (!wantsAir) {
      return DeviceStatus.READY;
    }
    if (tower.isEmpty()) {
      return DeviceStatus.NO_TOWER;
    }
    if (!tower.get().tallEnough(settings)) {
      return DeviceStatus.TOWER_TOO_SHORT;
    }
    if (!tower.get().capped()) {
      return DeviceStatus.TOWER_NO_ROD;
    }
    return inUse ? DeviceStatus.TOWER_IN_USE : DeviceStatus.RUNNING;
  }

  public static boolean wasCutOff(long now, long lastLive) {
    return now - lastLive > LONGEST_LIVE_GAP;
  }

  public static long onAirSinceAfterBeat(long onAirSince, boolean live, long now, long lastLive) {
    if (!live) {
      return NOT_ON_AIR;
    }
    return onAirSince == NOT_ON_AIR || wasCutOff(now, lastLive) ? now : onAirSince;
  }

  public static long songStartAfterBeat(
      boolean songWanted, long songStart, DeviceStatus status, long now, long lastLive) {
    if (!songWanted || status != DeviceStatus.RUNNING) {
      return SongClock.STOPPED;
    }
    return songStart == SongClock.STOPPED || wasCutOff(now, lastLive) ? now : songStart;
  }
}
