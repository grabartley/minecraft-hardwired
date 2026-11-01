package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientRadioSettingsTest {

  @ParameterizedTest(name = "noise {0} clamps to {1}")
  @CsvSource({"-5,0", "50,50", "500,100"})
  void noiseIsClamped(int noise, int expected) {
    assertEquals(expected, new ClientRadioSettings("", true, noise).radioNoisePercent());
  }

  @ParameterizedTest(name = "mic {0} hear {1} noise {2}")
  @CsvSource({"USB Mic,true,50", "'',false,0", "Built-in Microphone,false,100"})
  void settingsSurviveJson(String mic, boolean hear, int noise) {
    ClientRadioSettings settings = new ClientRadioSettings(mic, hear, noise);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, ClientRadioSettings.fromJson(root));
  }

  @ParameterizedTest(name = "json {0}")
  @CsvSource(
      delimiter = '|',
      value = {"{}", "{\"microphone\":5,\"hearVoiceBroadcasts\":\"yes\",\"radioNoise\":\"loud\"}"})
  void missingOrBadFieldsFallBackToDefaults(String json) {
    assertEquals(
        ClientRadioSettings.DEFAULTS,
        ClientRadioSettings.fromJson(JsonParser.parseString(json).getAsJsonObject()));
  }

  @ParameterizedTest(name = "with changes one field to {0}")
  @CsvSource({"7", "90"})
  void eachWitherChangesOneField(int value) {
    ClientRadioSettings d = ClientRadioSettings.DEFAULTS;
    assertEquals(value, d.withRadioNoisePercent(value).radioNoisePercent());
    assertEquals("mic" + value, d.withMicrophone("mic" + value).microphone());
    assertEquals(false, d.withHearVoiceBroadcasts(false).hearVoiceBroadcasts());
  }
}
