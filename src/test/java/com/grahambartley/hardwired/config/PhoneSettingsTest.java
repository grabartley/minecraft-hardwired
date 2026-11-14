package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneSettingsTest {

  @ParameterizedTest(name = "ring {0} lines {1}")
  @CsvSource({"0,0,5,1", "30,4,30,4", "9999,99,300,12"})
  void valuesAreClamped(int ring, int lines, int expectedRing, int expectedLines) {
    PhoneSettings settings = new PhoneSettings(ring, lines, true);
    assertEquals(expectedRing, settings.phoneRingSeconds());
    assertEquals(expectedLines, settings.exchangeLines());
  }

  @ParameterizedTest(name = "ring {0} lines {1} voice {2}")
  @CsvSource({"12,2,false", "300,12,true"})
  void settingsSurviveAJsonRoundTrip(int ring, int lines, boolean voice) {
    PhoneSettings settings = new PhoneSettings(ring, lines, voice);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, PhoneSettings.fromJson(new ConfigJsonFields(root)));
  }
}
