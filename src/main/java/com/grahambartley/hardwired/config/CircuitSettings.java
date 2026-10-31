package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record CircuitSettings(int maxParts, int partsPerRedstone) {

  public static final int MAX_PARTS_MIN = 8;
  public static final int MAX_PARTS_MAX = 256;
  public static final int PARTS_PER_REDSTONE_MIN = 1;
  public static final int PARTS_PER_REDSTONE_MAX = 64;

  public static final CircuitSettings DEFAULTS = new CircuitSettings(64, 4);

  static final String KEY_MAX_PARTS = "maxParts";
  static final String KEY_PARTS_PER_REDSTONE = "partsPerRedstone";

  public CircuitSettings {
    maxParts = ConfigBounds.clamp(maxParts, MAX_PARTS_MIN, MAX_PARTS_MAX);
    partsPerRedstone =
        ConfigBounds.clamp(partsPerRedstone, PARTS_PER_REDSTONE_MIN, PARTS_PER_REDSTONE_MAX);
  }

  public CircuitSettings withMaxParts(final int value) {
    return new CircuitSettings(value, partsPerRedstone);
  }

  public CircuitSettings withPartsPerRedstone(final int value) {
    return new CircuitSettings(maxParts, value);
  }

  static CircuitSettings fromJson(final ConfigJsonFields fields) {
    return new CircuitSettings(
        fields.readInt(KEY_MAX_PARTS, DEFAULTS.maxParts, MAX_PARTS_MIN, MAX_PARTS_MAX),
        fields.readInt(
            KEY_PARTS_PER_REDSTONE,
            DEFAULTS.partsPerRedstone,
            PARTS_PER_REDSTONE_MIN,
            PARTS_PER_REDSTONE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_MAX_PARTS, maxParts);
    root.addProperty(KEY_PARTS_PER_REDSTONE, partsPerRedstone);
  }
}
