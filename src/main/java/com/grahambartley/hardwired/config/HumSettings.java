package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record HumSettings(int humRange, int humStartLoad, int humFullLoad) {

  public static final int HUM_RANGE_MIN = 4;
  public static final int HUM_RANGE_MAX = 64;
  public static final int HUM_START_LOAD_MIN = 0;
  public static final int HUM_START_LOAD_MAX = 4096;
  public static final int HUM_FULL_LOAD_MIN = 1;
  public static final int HUM_FULL_LOAD_MAX = 4096;

  public static final HumSettings DEFAULTS = new HumSettings(16, 24, 128);

  static final String KEY_HUM_RANGE = "humRange";
  static final String KEY_HUM_START_LOAD = "humStartLoad";
  static final String KEY_HUM_FULL_LOAD = "humFullLoad";

  public HumSettings {
    humRange = ConfigBounds.clamp(humRange, HUM_RANGE_MIN, HUM_RANGE_MAX);
    humStartLoad = ConfigBounds.clamp(humStartLoad, HUM_START_LOAD_MIN, HUM_START_LOAD_MAX);
    humFullLoad = ConfigBounds.clamp(humFullLoad, HUM_FULL_LOAD_MIN, HUM_FULL_LOAD_MAX);
  }

  public HumSettings withHumRange(final int value) {
    return new HumSettings(value, humStartLoad, humFullLoad);
  }

  public HumSettings withHumStartLoad(final int value) {
    return new HumSettings(humRange, value, humFullLoad);
  }

  public HumSettings withHumFullLoad(final int value) {
    return new HumSettings(humRange, humStartLoad, value);
  }

  static HumSettings fromJson(final ConfigJsonFields fields) {
    return new HumSettings(
        fields.readInt(KEY_HUM_RANGE, DEFAULTS.humRange, HUM_RANGE_MIN, HUM_RANGE_MAX),
        fields.readInt(
            KEY_HUM_START_LOAD, DEFAULTS.humStartLoad, HUM_START_LOAD_MIN, HUM_START_LOAD_MAX),
        fields.readInt(
            KEY_HUM_FULL_LOAD, DEFAULTS.humFullLoad, HUM_FULL_LOAD_MIN, HUM_FULL_LOAD_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_HUM_RANGE, humRange);
    root.addProperty(KEY_HUM_START_LOAD, humStartLoad);
    root.addProperty(KEY_HUM_FULL_LOAD, humFullLoad);
  }
}
