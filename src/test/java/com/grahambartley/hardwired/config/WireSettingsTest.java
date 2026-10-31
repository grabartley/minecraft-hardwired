package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WireSettingsTest {

  @ParameterizedTest(name = "range {0} network {1}")
  @CsvSource({"0,0,8,64", "32,8192,32,8192", "999,999999,96,65536"})
  void valuesAreClamped(int range, int size, int expectedRange, int expectedSize) {
    WireSettings settings = new WireSettings(range, size);
    assertEquals(expectedRange, settings.viewRangeBlocks());
    assertEquals(expectedSize, settings.maxNetworkSize());
  }

  @ParameterizedTest(name = "setting {0} keeps the other field")
  @CsvSource({"16", "64"})
  void withersChangeOneField(int value) {
    assertEquals(
        WireSettings.DEFAULTS.maxNetworkSize(),
        WireSettings.DEFAULTS.withViewRangeBlocks(value).maxNetworkSize());
  }
}
