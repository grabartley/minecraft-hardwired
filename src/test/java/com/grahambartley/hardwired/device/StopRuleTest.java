package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.power.SourceState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StopRuleTest {

  @ParameterizedTest(name = "powered {0} on {1}, grid says {2}")
  @CsvSource({
    "true,true,RUNNING,RUNNING",
    "true,false,RUNNING,WIRE_OFF",
    "false,true,OVERLOADED,OVERLOADED",
    "false,false,NO_SOURCE,NO_SOURCE",
    "false,true,POWER_RETURNING,POWER_RETURNING"
  })
  void aDeviceGivesTheGridsReasonOnlyWithoutPower(
      boolean powered, boolean on, DeviceStatus grid, DeviceStatus status) {
    assertEquals(status, StopRule.ofDevice(powered, on, grid));
  }

  @ParameterizedTest(name = "{0} wired {1}, grid says {2}")
  @CsvSource({
    "SWITCHED_OFF,true,RUNNING,SOURCES_OFF",
    "NO_FUEL,true,RUNNING,OUT_OF_FUEL",
    "READY,true,OVERLOADED,OVERLOADED",
    "READY_FREE,true,SOURCES_OFF,SOURCES_OFF",
    "READY,true,RUNNING,RUNNING",
    "READY,false,NO_SOURCE,READY"
  })
  void aSourceGivesItsOwnReasonFirst(
      SourceState own, boolean wired, DeviceStatus grid, DeviceStatus status) {
    assertEquals(status, StopRule.ofSource(own, wired, grid));
  }
}
