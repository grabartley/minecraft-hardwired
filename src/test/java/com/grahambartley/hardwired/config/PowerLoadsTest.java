package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PowerLoadsTest {

  static Stream<Arguments> agreedDefaults() {
    return Stream.of(
        Arguments.of("lampLoad", 1, (ToIntFunction<PowerLoads>) PowerLoads::lampLoad),
        Arguments.of("streetlightLoad", 2, (ToIntFunction<PowerLoads>) PowerLoads::streetlightLoad),
        Arguments.of("logicLoad", 2, (ToIntFunction<PowerLoads>) PowerLoads::logicLoad),
        Arguments.of("vanillaLoad", 2, (ToIntFunction<PowerLoads>) PowerLoads::vanillaLoad),
        Arguments.of("radioLoad", 4, (ToIntFunction<PowerLoads>) PowerLoads::radioLoad),
        Arguments.of("speakerLoad", 4, (ToIntFunction<PowerLoads>) PowerLoads::speakerLoad),
        Arguments.of("intercomLoad", 4, (ToIntFunction<PowerLoads>) PowerLoads::intercomLoad),
        Arguments.of("alarmLoad", 8, (ToIntFunction<PowerLoads>) PowerLoads::alarmLoad),
        Arguments.of("broadcastLoad", 16, (ToIntFunction<PowerLoads>) PowerLoads::broadcastLoad),
        Arguments.of("liftLoad", 32, (ToIntFunction<PowerLoads>) PowerLoads::liftLoad),
        Arguments.of("telephoneLoad", 1, (ToIntFunction<PowerLoads>) PowerLoads::telephoneLoad),
        Arguments.of("exchangeLoad", 4, (ToIntFunction<PowerLoads>) PowerLoads::exchangeLoad),
        Arguments.of("bellBoxLoad", 1, (ToIntFunction<PowerLoads>) PowerLoads::bellBoxLoad));
  }

  @ParameterizedTest(name = "{0} defaults to {1}")
  @MethodSource("agreedDefaults")
  void defaultsAreTheAgreedBalance(String field, int value, ToIntFunction<PowerLoads> read) {
    assertEquals(value, read.applyAsInt(PowerLoads.DEFAULTS));
  }

  @ParameterizedTest(name = "{0} clamps to {1}")
  @CsvSource({"-1000000,0", "2000000,256"})
  void valuesAreClamped(int value, int clamped) {
    assertEquals(clamped, PowerLoads.DEFAULTS.withLiftLoad(value).liftLoad());
  }

  @ParameterizedTest(name = "{0} survives json")
  @CsvSource({"0", "256"})
  void settingsSurviveJson(int value) {
    PowerLoads settings = PowerLoads.DEFAULTS.withLiftLoad(value);
    JsonObject root = new JsonObject();
    settings.writeJson(root);
    assertEquals(settings, PowerLoads.fromJson(new ConfigJsonFields(root)));
  }
}
