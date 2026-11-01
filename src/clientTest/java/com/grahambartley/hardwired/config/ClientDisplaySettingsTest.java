package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientDisplaySettingsTest {

  @ParameterizedTest(name = "{0},{1} clamps to {2},{3}")
  @CsvSource({"0,0,10,30", "44,100,44,100", "999,999,100,100"})
  void valuesAreClamped(int through, int surface, int expectedThrough, int expectedSurface) {
    ClientDisplaySettings settings = new ClientDisplaySettings(through, surface, true, true);
    assertEquals(expectedThrough, settings.throughBlocksPercent());
    assertEquals(expectedSurface, settings.onSurfacePercent());
  }

  @ParameterizedTest(name = "{0}% is alpha {1}")
  @CsvSource({"100,255", "44,112", "10,25"})
  void percentBecomesAnAlphaByte(int percent, int alpha) {
    assertEquals(
        alpha, new ClientDisplaySettings(percent, 100, true, true).throughBlocksAlpha() >>> 24);
  }

  @ParameterizedTest(name = "markers json {0}")
  @ValueSource(strings = {"{}", "{\"showMarkers\":3}", "{\"showMarkers\":[1]}"})
  void badMarkerValueKeepsDefault(String json) {
    assertTrue(
        ClientDisplaySettings.fromJson(JsonParser.parseString(json).getAsJsonObject())
            .showMarkers());
  }

  @ParameterizedTest(name = "coloured light json {0} reads {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "{}|true",
        "{\"colouredLight\":3}|true",
        "{\"colouredLight\":false}|false",
        "{\"colouredLight\":true}|true"
      })
  void colouredLightReadsFromJson(String json, boolean expected) {
    assertEquals(
        expected,
        ClientDisplaySettings.fromJson(JsonParser.parseString(json).getAsJsonObject())
            .colouredLight());
  }
}
