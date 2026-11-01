package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeskRunTest {

  @ParameterizedTest(name = "{0} wants {1} masts {2} rod {3} used {4}")
  @CsvSource({
    "false,5,true,false,READY",
    "true,0,false,false,NO_TOWER",
    "true,4,true,false,TOWER_TOO_SHORT",
    "true,5,false,false,TOWER_NO_ROD",
    "true,5,true,true,TOWER_IN_USE",
    "true,5,true,false,RUNNING"
  })
  void aDeskSaysWhyItIsNotOnTheAir(
      boolean wantsAir, int masts, boolean rod, boolean inUse, DeviceStatus expected) {
    Optional<TowerColumn> tower =
        masts == 0 ? Optional.empty() : Optional.of(new TowerColumn(64, masts, rod));
    assertEquals(expected, DeskRun.demand(wantsAir, tower, inUse, RadioSettings.DEFAULTS));
  }

  @ParameterizedTest(name = "wanted {0} start {1} status {2} now {3} last {4}")
  @CsvSource({
    "false,-1,RUNNING,1000,980,-1",
    "true,900,RUNNING,1000,980,900",
    "true,-1,RUNNING,1000,980,1000",
    "true,900,NO_CHARGE,1000,980,-1",
    "true,900,RUNNING,5000,980,5000"
  })
  void aSongCarriesOnOnlyWhileTheStationStaysLive(
      boolean wanted, long start, DeviceStatus status, long now, long lastLive, long expected) {
    assertEquals(expected, DeskRun.songStartAfterBeat(wanted, start, status, now, lastLive));
  }

  @ParameterizedTest(name = "since {0} live {1} now {2} last {3}")
  @CsvSource({
    "-1,true,1000,-1,1000",
    "500,true,1000,980,500",
    "500,true,5000,980,5000",
    "500,false,1000,980,-1"
  })
  void timeOnTheAirRestartsAfterACutOff(
      long since, boolean live, long now, long lastLive, long expected) {
    assertEquals(expected, DeskRun.onAirSinceAfterBeat(since, live, now, lastLive));
  }
}
