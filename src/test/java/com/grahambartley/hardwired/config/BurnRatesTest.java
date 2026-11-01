package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BurnRatesTest {

  static Stream<Arguments> rates() {
    return Stream.of(
        rate("cell", 2, BurnRates::cellChargePerLoadMinute, BurnRates::withCellChargePerLoadMinute),
        rate(
            "generator",
            1,
            BurnRates::generatorChargePerLoadMinute,
            BurnRates::withGeneratorChargePerLoadMinute),
        rate(
            "spare",
            2,
            BurnRates::spareChargePerLoadMinute,
            BurnRates::withSpareChargePerLoadMinute));
  }

  private static Arguments rate(
      String name,
      int value,
      ToIntFunction<BurnRates> read,
      BiFunction<BurnRates, Integer, BurnRates> write) {
    return Arguments.of(name, value, read, write);
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("rates")
  void defaultsAreTheAgreedBalance(
      String name,
      int value,
      ToIntFunction<BurnRates> read,
      BiFunction<BurnRates, Integer, BurnRates> write) {
    assertEquals(value, read.applyAsInt(BurnRates.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} is clamped and survives json on its own key")
  @MethodSource("rates")
  void eachRateIsClampedAndSurvivesJson(
      String name,
      int value,
      ToIntFunction<BurnRates> read,
      BiFunction<BurnRates, Integer, BurnRates> write) {
    assertEquals(0, read.applyAsInt(write.apply(BurnRates.DEFAULTS, -5)));
    assertEquals(60, read.applyAsInt(write.apply(BurnRates.DEFAULTS, 5000)));
    BurnRates rates = write.apply(BurnRates.DEFAULTS, 17);
    JsonObject root = new JsonObject();
    rates.writeJson(root);
    assertEquals(rates, BurnRates.fromJson(new ConfigJsonFields(root)));
  }
}
