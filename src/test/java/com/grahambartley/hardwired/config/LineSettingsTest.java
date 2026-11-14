package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LineSettingsTest {

  @ParameterizedTest(name = "span {0} per wire {1}")
  @CsvSource({"0,0,8,1", "32,4,32,4", "9999,99,64,32"})
  void valuesAreClamped(int span, int perWire, int expectedSpan, int expectedPerWire) {
    LineSettings settings = new LineSettings(span, perWire);
    assertEquals(expectedSpan, settings.poleSpanBlocks());
    assertEquals(expectedPerWire, settings.spanBlocksPerWire());
  }

  @ParameterizedTest(name = "span {0} per wire {1}")
  @CsvSource({"8,1", "48,6", "64,32"})
  void settingsSurviveAJsonRoundTrip(int span, int perWire) {
    LineSettings settings = new LineSettings(span, perWire);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, LineSettings.fromJson(new ConfigJsonFields(root)));
  }
}
