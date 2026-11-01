package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class BurnRatesTest {

  private static final int MINUTES_PER_DAY = 20;
  private static final int STACK = 64;

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

  @ParameterizedTest(name = "a stack of dust runs {0} load on {1} for a game day")
  @CsvSource({"16,cell", "32,generator"})
  void aStackOfDustRunsAHouseForADay(int load, String source) {
    PowerSettings power = PowerSettings.DEFAULTS;
    int rate =
        source.equals("cell")
            ? power.burn().cellChargePerLoadMinute()
            : power.burn().generatorChargePerLoadMinute();
    double minutes = STACK * power.dustCharge() / (double) (load * rate);
    assertEquals(1D, minutes / MINUTES_PER_DAY, 0.05);
  }

  @ParameterizedTest(name = "a full {0} at its rating lasts {1} game days")
  @CsvSource({"cell,1.5", "generator,3"})
  void aFullSourceAtItsRatingLasts(String source, double days) {
    PowerSettings power = PowerSettings.DEFAULTS;
    boolean cell = source.equals("cell");
    int capacity = cell ? power.cellCapacity() : power.generatorCapacity();
    int rating = cell ? power.ratings().cellRating() : power.ratings().generatorRating();
    int rate =
        cell ? power.burn().cellChargePerLoadMinute() : power.burn().generatorChargePerLoadMinute();
    assertEquals(days, capacity / (double) (rating * rate) / MINUTES_PER_DAY, 0.15);
  }
}
