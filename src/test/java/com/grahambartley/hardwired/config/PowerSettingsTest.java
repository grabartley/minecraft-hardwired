package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PowerSettingsTest {

  static Stream<Arguments> agreedDefaults() {
    return Stream.of(
        Arguments.of(
            "cellCapacity", 1000, (ToIntFunction<PowerSettings>) PowerSettings::cellCapacity),
        Arguments.of(
            "generatorCapacity",
            4000,
            (ToIntFunction<PowerSettings>) PowerSettings::generatorCapacity),
        Arguments.of("dustCharge", 10, (ToIntFunction<PowerSettings>) PowerSettings::dustCharge),
        Arguments.of(
            "lightningCharge", 500, (ToIntFunction<PowerSettings>) PowerSettings::lightningCharge));
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("agreedDefaults")
  void defaultsAreTheAgreedBalance(String name, int value, ToIntFunction<PowerSettings> read) {
    assertEquals(value, read.applyAsInt(PowerSettings.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} clamps")
  @CsvSource({"-1000000,100", "2000000,100000"})
  void valuesAreClamped(int value, int clamped) {
    assertEquals(clamped, PowerSettings.DEFAULTS.withCellCapacity(value).cellCapacity());
  }

  @ParameterizedTest(name = "lamp load {0} and rating {1} survive json flat")
  @CsvSource({"1,0,4", "7,40,16", "256,1024,64"})
  void nestedSettingsSurviveJsonOnTheFlatKeys(int lampLoad, int cellRating, int humRange) {
    PowerSettings settings =
        PowerSettings.DEFAULTS
            .withLoads(PowerLoads.DEFAULTS.withLampLoad(lampLoad))
            .withRatings(PowerRatings.DEFAULTS.withCellRating(cellRating))
            .withHum(HumSettings.DEFAULTS.withHumRange(humRange))
            .withBurn(BurnRates.DEFAULTS.withSpareChargePerLoadMinute(humRange));
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(lampLoad, root.get("lampLoad").getAsInt());
    assertEquals(settings, PowerSettings.fromJson(new ConfigJsonFields(root)));
  }
}
