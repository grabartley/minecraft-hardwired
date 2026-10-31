package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record LampSettings(int ageRateMultiplierPercent) {

  public static final int AGE_RATE_MIN = 0;
  public static final int AGE_RATE_MAX = 1000;

  public static final LampSettings DEFAULTS = new LampSettings(100);

  static final String KEY_AGE_RATE_MULTIPLIER_PERCENT = "lampAgeRateMultiplierPercent";

  public LampSettings {
    ageRateMultiplierPercent =
        ConfigBounds.clamp(ageRateMultiplierPercent, AGE_RATE_MIN, AGE_RATE_MAX);
  }

  public LampSettings withAgeRateMultiplierPercent(final int value) {
    return new LampSettings(value);
  }

  static LampSettings fromJson(final ConfigJsonFields fields) {
    return new LampSettings(
        fields.readInt(
            KEY_AGE_RATE_MULTIPLIER_PERCENT,
            DEFAULTS.ageRateMultiplierPercent,
            AGE_RATE_MIN,
            AGE_RATE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_AGE_RATE_MULTIPLIER_PERCENT, ageRateMultiplierPercent);
  }
}
