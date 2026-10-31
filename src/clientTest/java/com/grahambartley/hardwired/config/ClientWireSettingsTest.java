package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClientWireSettingsTest {

  @ParameterizedTest(name = "{0},{1} clamps to {2},{3}")
  @CsvSource({"0,0,10,30", "44,100,44,100", "999,999,100,100"})
  void valuesAreClamped(int through, int surface, int expectedThrough, int expectedSurface) {
    ClientWireSettings settings = new ClientWireSettings(through, surface, true);
    assertEquals(expectedThrough, settings.throughBlocksPercent());
    assertEquals(expectedSurface, settings.onSurfacePercent());
  }

  @ParameterizedTest(name = "{0}% is alpha {1}")
  @CsvSource({"100,255", "44,112", "10,25"})
  void percentBecomesAnAlphaByte(int percent, int alpha) {
    assertEquals(alpha, new ClientWireSettings(percent, 100, true).throughBlocksAlpha() >>> 24);
  }

  @ParameterizedTest(name = "markers json {0}")
  @ValueSource(strings = {"{}", "{\"showMarkers\":3}", "{\"showMarkers\":[1]}"})
  void badMarkerValueKeepsDefault(String json) {
    assertTrue(
        ClientWireSettings.fromJson(JsonParser.parseString(json).getAsJsonObject()).showMarkers());
  }
}
