package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DeviceStatusTest {

  @ParameterizedTest(name = "{0}")
  @EnumSource(DeviceStatus.class)
  void everyStatusSurvivesItsSavedId(DeviceStatus status) {
    assertEquals(status, DeviceStatus.byId(status.id(), DeviceStatus.READY));
  }

  @ParameterizedTest(name = "case {index}")
  @CsvSource({"'',READY", "nonsense,WIRE_OFF"})
  void anUnknownIdFallsBack(String id, DeviceStatus fallback) {
    assertEquals(fallback, DeviceStatus.byId(id, fallback));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "RUNNING,false,true",
    "READY,false,false",
    "WIRE_OFF,true,false",
    "NO_CELL,true,true",
    "NO_CHARGE,true,true",
    "NO_DISC,true,false",
    "NO_SPEAKER,true,false",
    "NO_RADIO,true,false",
    "SONG_ENDED,true,false"
  })
  void statusSaysWhetherAReasonShowsAndWhetherChargeIsWanted(
      DeviceStatus status, boolean stopped, boolean wantsCharge) {
    assertEquals(stopped, status.isStopped());
    assertEquals(wantsCharge, status.wantsCharge());
  }
}
