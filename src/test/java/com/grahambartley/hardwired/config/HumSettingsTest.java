package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class HumSettingsTest {

  static Stream<Arguments> agreedDefaults() {
    return Stream.of(
        Arguments.of("humRange", 16, (ToIntFunction<HumSettings>) HumSettings::humRange),
        Arguments.of("humStartLoad", 24, (ToIntFunction<HumSettings>) HumSettings::humStartLoad),
        Arguments.of("humFullLoad", 128, (ToIntFunction<HumSettings>) HumSettings::humFullLoad));
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("agreedDefaults")
  void defaultsAreTheAgreedBalance(String field, int value, ToIntFunction<HumSettings> read) {
    assertEquals(value, read.applyAsInt(HumSettings.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,4", "2000000,64"})
  void valuesAreClamped(int value, int clamped) {
    assertEquals(clamped, HumSettings.DEFAULTS.withHumRange(value).humRange());
  }

  @ParameterizedTest(name = "{0} survives json")
  @CsvSource({"4", "64"})
  void settingsSurviveJson(int value) {
    HumSettings settings = HumSettings.DEFAULTS.withHumRange(value);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, HumSettings.fromJson(new ConfigJsonFields(root)));
  }
}
