package com.grahambartley.hardwired.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConfigJsonFieldsTest {

  @ParameterizedTest(name = "{0} reads as {1}")
  @CsvSource(
      delimiter = '|',
      value = {"{\"a\": 7}|7", "{}|3", "{\"a\": \"x\"}|3", "{\"a\": true}|3", "{\"a\": null}|3"})
  void readsNumbersAndFallsBack(String json, int expected) {
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    assertEquals(expected, new ConfigJsonFields(root).readInt("a", 3));
  }
}
