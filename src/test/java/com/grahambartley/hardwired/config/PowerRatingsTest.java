package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PowerRatingsTest {

  static Stream<Arguments> agreedDefaults() {
    return Stream.of(
        Arguments.of("cellRating", 16, (ToIntFunction<PowerRatings>) PowerRatings::cellRating),
        Arguments.of(
            "generatorRating", 64, (ToIntFunction<PowerRatings>) PowerRatings::generatorRating),
        Arguments.of("solarRating", 6, (ToIntFunction<PowerRatings>) PowerRatings::solarRating),
        Arguments.of("dynamoRating", 24, (ToIntFunction<PowerRatings>) PowerRatings::dynamoRating));
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("agreedDefaults")
  void defaultsAreTheAgreedBalance(String field, int value, ToIntFunction<PowerRatings> read) {
    assertEquals(value, read.applyAsInt(PowerRatings.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,0", "2000000,1024"})
  void valuesAreClamped(int value, int clamped) {
    assertEquals(clamped, PowerRatings.DEFAULTS.withCellRating(value).cellRating());
  }

  @ParameterizedTest(name = "{0} survives json")
  @CsvSource({"0", "1024"})
  void settingsSurviveJson(int value) {
    PowerRatings settings = PowerRatings.DEFAULTS.withCellRating(value);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, PowerRatings.fromJson(new ConfigJsonFields(root)));
  }
}
