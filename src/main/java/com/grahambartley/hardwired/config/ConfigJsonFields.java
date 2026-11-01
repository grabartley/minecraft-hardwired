package com.grahambartley.hardwired.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfigJsonFields {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigJsonFields.class);

  private final JsonObject root;

  ConfigJsonFields(final JsonObject root) {
    this.root = root;
  }

  int readInt(final String key, final int fallback) {
    return (int) readNumber(key, fallback);
  }

  int readInt(final String key, final int fallback, final int min, final int max) {
    final int value = readInt(key, fallback);
    ConfigBounds.warnIfOutside(key, value, min, max);
    return value;
  }

  boolean readBoolean(final String key, final boolean fallback) {
    if (!root.has(key)) {
      return fallback;
    }
    final JsonElement element = root.get(key);
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
      return element.getAsBoolean();
    }
    warnInvalid(key, element, "true or false", fallback);
    return fallback;
  }

  private double readNumber(final String key, final double fallback) {
    if (!root.has(key)) {
      return fallback;
    }
    final JsonElement element = root.get(key);
    final OptionalDouble number = finiteNumber(element);
    if (number.isPresent()) {
      return number.getAsDouble();
    }
    warnInvalid(key, element, "a number", fallback);
    return fallback;
  }

  private static OptionalDouble finiteNumber(final JsonElement element) {
    if (!element.isJsonPrimitive()) {
      return OptionalDouble.empty();
    }
    final JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (!primitive.isNumber()) {
      return OptionalDouble.empty();
    }
    final double value = primitive.getAsDouble();
    return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
  }

  private static void warnInvalid(
      final String key, final JsonElement element, final String expected, final Object fallback) {
    LOGGER.warn(
        "Hardwired config value {}={} is not {}, using {}", key, element, expected, fallback);
  }
}
