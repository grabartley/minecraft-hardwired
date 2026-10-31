package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceSettingsTest {

  @ParameterizedTest(name = "range {0} hold {1}")
  @CsvSource({"0,0,1,2", "6,20,6,20", "99,999,16,200"})
  void valuesAreClamped(int range, int hold, int expectedRange, int expectedHold) {
    DeviceSettings settings = new DeviceSettings(range, hold);
    assertEquals(expectedRange, settings.motionSensorRangeBlocks());
    assertEquals(expectedHold, settings.buttonHoldTicks());
  }
}
