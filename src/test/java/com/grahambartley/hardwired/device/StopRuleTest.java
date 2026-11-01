package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.power.SourceState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StopRuleTest {

  @ParameterizedTest(name = "powered {0} on {1} needs wire {2}, grid says {3}")
  @CsvSource({
    "true,true,true,RUNNING,RUNNING",
    "true,false,true,RUNNING,WIRE_OFF",
    "true,false,false,RUNNING,RUNNING",
    "false,true,true,OVERLOADED,OVERLOADED",
    "false,false,false,NO_SOURCE,NO_SOURCE",
    "false,true,true,POWER_RETURNING,POWER_RETURNING"
  })
  void aDeviceGivesTheGridsReasonOnlyWithoutPower(
      boolean powered, boolean on, boolean needsWire, DeviceStatus grid, DeviceStatus status) {
    assertEquals(status, StopRule.ofDevice(powered, on, needsWire, grid));
  }

  @ParameterizedTest(name = "{0} overloaded {1} totals {2}")
  @CsvSource({
    "OVERLOADED,true,false,true",
    "OVERLOADED,true,true,false",
    "SOURCES_OFF,false,true,true",
    "NO_SOURCE,false,false,true",
    "RUNNING,false,false,false",
    "READY,false,true,false"
  })
  void theHudNeverHidesWhyADeviceStopped(
      DeviceStatus status, boolean overloaded, boolean totals, boolean shown) {
    assertEquals(shown, StopRule.showsStatus(status, overloaded, totals));
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
