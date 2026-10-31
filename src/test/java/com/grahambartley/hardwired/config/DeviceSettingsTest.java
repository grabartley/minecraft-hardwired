package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceSettingsTest {

  @ParameterizedTest(name = "range {0} hold {1} intercom {2}")
  @CsvSource({"0,0,0,1,2,1", "6,20,16,6,20,16", "99,999,999,16,200,128"})
  void valuesAreClamped(
      int range,
      int hold,
      int intercom,
      int expectedRange,
      int expectedHold,
      int expectedIntercom) {
    DeviceSettings settings = new DeviceSettings(range, hold, intercom);
    assertEquals(expectedRange, settings.motionSensorRangeBlocks());
    assertEquals(expectedHold, settings.buttonHoldTicks());
    assertEquals(expectedIntercom, settings.intercomRangeBlocks());
  }
}
