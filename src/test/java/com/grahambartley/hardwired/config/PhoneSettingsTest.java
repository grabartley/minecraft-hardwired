package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneSettingsTest {

  @ParameterizedTest(name = "ring {0} lines {1} bell {2}")
  @CsvSource({"0,0,0,5,1,1", "30,4,32,30,4,32", "9999,99,9999,300,12,256"})
  void valuesAreClamped(
      int ring, int lines, int bell, int expectedRing, int expectedLines, int expectedBell) {
    PhoneSettings settings = new PhoneSettings(ring, lines, bell, true);
    assertEquals(expectedRing, settings.phoneRingSeconds());
    assertEquals(expectedLines, settings.exchangeLines());
    assertEquals(expectedBell, settings.bellBoxHearing());
  }

  @ParameterizedTest(name = "ring {0} lines {1} bell {2} voice {3}")
  @CsvSource({"12,2,40,false", "300,12,256,true"})
  void settingsSurviveAJsonRoundTrip(int ring, int lines, int bell, boolean voice) {
    PhoneSettings settings = new PhoneSettings(ring, lines, bell, voice);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, PhoneSettings.fromJson(new ConfigJsonFields(root)));
  }
}
