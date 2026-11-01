package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ElectronicsRuleTest {

  @ParameterizedTest(name = "{0} powered {1}")
  @CsvSource({
    "RUNNING,true,OVERLOADED,RUNNING",
    "RUNNING,false,OVERLOADED,OVERLOADED",
    "RUNNING,false,NO_SOURCE,NO_SOURCE",
    "WIRE_OFF,false,NO_SOURCE,WIRE_OFF",
    "NO_TOWER,false,SOURCES_OFF,NO_TOWER",
    "RADIO_OFF,true,OUT_OF_FUEL,RADIO_OFF"
  })
  void aRunningDeviceWithNoPowerStopsWithItsGridsReason(
      DeviceStatus demand, boolean powered, DeviceStatus reason, DeviceStatus next) {
    assertEquals(next, ElectronicsRule.next(demand, powered, reason));
  }

  @ParameterizedTest(name = "beat at {0}, now {1}")
  @CsvSource({"100,100,false", "100,119,false", "100,120,true", "100,500,true", "-20,0,true"})
  void aBeatFallsOnceASecond(long beatAt, long now, boolean due) {
    assertEquals(due, ElectronicsRule.beatDue(beatAt, now));
  }
}
