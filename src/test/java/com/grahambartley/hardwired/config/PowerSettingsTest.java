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

  private static Stream<Arguments> agreedDefaults() {
    return Stream.of(
        Arguments.of("cellRating", 16, (ToIntFunction<PowerSettings>) PowerSettings::cellRating),
        Arguments.of(
            "generatorRating", 64, (ToIntFunction<PowerSettings>) PowerSettings::generatorRating),
        Arguments.of("solarRating", 6, (ToIntFunction<PowerSettings>) PowerSettings::solarRating),
        Arguments.of(
            "dynamoRating", 24, (ToIntFunction<PowerSettings>) PowerSettings::dynamoRating),
        Arguments.of("lampLoad", 1, (ToIntFunction<PowerSettings>) PowerSettings::lampLoad),
        Arguments.of(
            "streetlightLoad", 2, (ToIntFunction<PowerSettings>) PowerSettings::streetlightLoad),
        Arguments.of("logicLoad", 2, (ToIntFunction<PowerSettings>) PowerSettings::logicLoad),
        Arguments.of("vanillaLoad", 2, (ToIntFunction<PowerSettings>) PowerSettings::vanillaLoad),
        Arguments.of("radioLoad", 4, (ToIntFunction<PowerSettings>) PowerSettings::radioLoad),
        Arguments.of("alarmLoad", 8, (ToIntFunction<PowerSettings>) PowerSettings::alarmLoad),
        Arguments.of(
            "broadcastLoad", 16, (ToIntFunction<PowerSettings>) PowerSettings::broadcastLoad),
        Arguments.of("liftLoad", 32, (ToIntFunction<PowerSettings>) PowerSettings::liftLoad),
        Arguments.of(
            "generatorCapacity",
            4000,
            (ToIntFunction<PowerSettings>) PowerSettings::generatorCapacity),
        Arguments.of(
            "chargePerLoadMinute",
            3,
            (ToIntFunction<PowerSettings>) PowerSettings::chargePerLoadMinute),
        Arguments.of("humRange", 16, (ToIntFunction<PowerSettings>) PowerSettings::humRange),
        Arguments.of(
            "humStartLoad", 24, (ToIntFunction<PowerSettings>) PowerSettings::humStartLoad),
        Arguments.of(
            "humFullLoad", 128, (ToIntFunction<PowerSettings>) PowerSettings::humFullLoad));
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("agreedDefaults")
  void defaultsAreTheAgreedBalance(String name, int value, ToIntFunction<PowerSettings> read) {
    assertEquals(value, read.applyAsInt(PowerSettings.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} clamps")
  @CsvSource({"-1000000,low", "2000000,high"})
  void valuesAreClamped(int value, String end) {
    PowerSettings s = PowerSettings.DEFAULTS.withCellRating(value).withLiftLoad(value);
    boolean low = end.equals("low");
    assertEquals(
        low ? PowerSettings.CELL_RATING_MIN : PowerSettings.CELL_RATING_MAX, s.cellRating());
    assertEquals(low ? PowerSettings.LIFT_LOAD_MIN : PowerSettings.LIFT_LOAD_MAX, s.liftLoad());
  }

  @ParameterizedTest(name = "lamp load {0} survives json")
  @CsvSource({"1", "7", "256"})
  void settingsSurviveJson(int lampLoad) {
    PowerSettings settings = PowerSettings.DEFAULTS.withLampLoad(lampLoad);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, PowerSettings.fromJson(new ConfigJsonFields(root)));
  }
}
