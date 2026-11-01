package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record BurnRates(
    int cellChargePerLoadMinute, int generatorChargePerLoadMinute, int spareChargePerLoadMinute) {

  public static final int CHARGE_PER_LOAD_MINUTE_MIN = 0;
  public static final int CHARGE_PER_LOAD_MINUTE_MAX = 60;

  public static final BurnRates DEFAULTS = new BurnRates(2, 1, 2);

  static final String KEY_CELL = "cellChargePerLoadMinute";
  static final String KEY_GENERATOR = "generatorChargePerLoadMinute";
  static final String KEY_SPARE = "spareChargePerLoadMinute";

  public BurnRates {
    cellChargePerLoadMinute = bounded(cellChargePerLoadMinute);
    generatorChargePerLoadMinute = bounded(generatorChargePerLoadMinute);
    spareChargePerLoadMinute = bounded(spareChargePerLoadMinute);
  }

  private static int bounded(final int value) {
    return ConfigBounds.clamp(value, CHARGE_PER_LOAD_MINUTE_MIN, CHARGE_PER_LOAD_MINUTE_MAX);
  }

  public BurnRates withCellChargePerLoadMinute(final int value) {
    return new BurnRates(value, generatorChargePerLoadMinute, spareChargePerLoadMinute);
  }

  public BurnRates withGeneratorChargePerLoadMinute(final int value) {
    return new BurnRates(cellChargePerLoadMinute, value, spareChargePerLoadMinute);
  }

  public BurnRates withSpareChargePerLoadMinute(final int value) {
    return new BurnRates(cellChargePerLoadMinute, generatorChargePerLoadMinute, value);
  }

  private static int read(final ConfigJsonFields fields, final String key, final int fallback) {
    return fields.readInt(key, fallback, CHARGE_PER_LOAD_MINUTE_MIN, CHARGE_PER_LOAD_MINUTE_MAX);
  }

  static BurnRates fromJson(final ConfigJsonFields fields) {
    return new BurnRates(
        read(fields, KEY_CELL, DEFAULTS.cellChargePerLoadMinute),
        read(fields, KEY_GENERATOR, DEFAULTS.generatorChargePerLoadMinute),
        read(fields, KEY_SPARE, DEFAULTS.spareChargePerLoadMinute));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_CELL, cellChargePerLoadMinute);
    root.addProperty(KEY_GENERATOR, generatorChargePerLoadMinute);
    root.addProperty(KEY_SPARE, spareChargePerLoadMinute);
  }
}
