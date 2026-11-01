package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PowerSettings(
    int cellCapacity,
    int generatorCapacity,
    int dustCharge,
    int lightningCharge,
    int chargePerLoadMinute,
    PowerRatings ratings,
    PowerLoads loads,
    HumSettings hum) {

  public static final int CELL_CAPACITY_MIN = 100;
  public static final int CELL_CAPACITY_MAX = 100000;
  public static final int GENERATOR_CAPACITY_MIN = 100;
  public static final int GENERATOR_CAPACITY_MAX = 100000;
  public static final int DUST_CHARGE_MIN = 1;
  public static final int DUST_CHARGE_MAX = 1000;
  public static final int LIGHTNING_CHARGE_MIN = 0;
  public static final int LIGHTNING_CHARGE_MAX = 100000;
  public static final int CHARGE_PER_LOAD_MINUTE_MIN = 0;
  public static final int CHARGE_PER_LOAD_MINUTE_MAX = 60;

  public static final PowerSettings DEFAULTS =
      new PowerSettings(
          1000, 4000, 10, 500, 3, PowerRatings.DEFAULTS, PowerLoads.DEFAULTS, HumSettings.DEFAULTS);

  static final String KEY_CELL_CAPACITY = "cellCapacity";
  static final String KEY_GENERATOR_CAPACITY = "generatorCapacity";
  static final String KEY_DUST_CHARGE = "dustCharge";
  static final String KEY_LIGHTNING_CHARGE = "lightningCharge";
  static final String KEY_CHARGE_PER_LOAD_MINUTE = "chargePerLoadMinute";

  public PowerSettings {
    cellCapacity = ConfigBounds.clamp(cellCapacity, CELL_CAPACITY_MIN, CELL_CAPACITY_MAX);
    generatorCapacity =
        ConfigBounds.clamp(generatorCapacity, GENERATOR_CAPACITY_MIN, GENERATOR_CAPACITY_MAX);
    dustCharge = ConfigBounds.clamp(dustCharge, DUST_CHARGE_MIN, DUST_CHARGE_MAX);
    lightningCharge =
        ConfigBounds.clamp(lightningCharge, LIGHTNING_CHARGE_MIN, LIGHTNING_CHARGE_MAX);
    chargePerLoadMinute =
        ConfigBounds.clamp(
            chargePerLoadMinute, CHARGE_PER_LOAD_MINUTE_MIN, CHARGE_PER_LOAD_MINUTE_MAX);
    ratings = ratings == null ? PowerRatings.DEFAULTS : ratings;
    loads = loads == null ? PowerLoads.DEFAULTS : loads;
    hum = hum == null ? HumSettings.DEFAULTS : hum;
  }

  public PowerSettings withCellCapacity(final int value) {
    return new PowerSettings(
        value,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        ratings,
        loads,
        hum);
  }

  public PowerSettings withGeneratorCapacity(final int value) {
    return new PowerSettings(
        cellCapacity, value, dustCharge, lightningCharge, chargePerLoadMinute, ratings, loads, hum);
  }

  public PowerSettings withDustCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        value,
        lightningCharge,
        chargePerLoadMinute,
        ratings,
        loads,
        hum);
  }

  public PowerSettings withLightningCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        value,
        chargePerLoadMinute,
        ratings,
        loads,
        hum);
  }

  public PowerSettings withChargePerLoadMinute(final int value) {
    return new PowerSettings(
        cellCapacity, generatorCapacity, dustCharge, lightningCharge, value, ratings, loads, hum);
  }

  public PowerSettings withRatings(final PowerRatings value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        value,
        loads,
        hum);
  }

  public PowerSettings withLoads(final PowerLoads value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        ratings,
        value,
        hum);
  }

  public PowerSettings withHum(final HumSettings value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        ratings,
        loads,
        value);
  }

  static PowerSettings fromJson(final ConfigJsonFields fields) {
    return new PowerSettings(
        fields.readInt(
            KEY_CELL_CAPACITY, DEFAULTS.cellCapacity, CELL_CAPACITY_MIN, CELL_CAPACITY_MAX),
        fields.readInt(
            KEY_GENERATOR_CAPACITY,
            DEFAULTS.generatorCapacity,
            GENERATOR_CAPACITY_MIN,
            GENERATOR_CAPACITY_MAX),
        fields.readInt(KEY_DUST_CHARGE, DEFAULTS.dustCharge, DUST_CHARGE_MIN, DUST_CHARGE_MAX),
        fields.readInt(
            KEY_LIGHTNING_CHARGE,
            DEFAULTS.lightningCharge,
            LIGHTNING_CHARGE_MIN,
            LIGHTNING_CHARGE_MAX),
        fields.readInt(
            KEY_CHARGE_PER_LOAD_MINUTE,
            DEFAULTS.chargePerLoadMinute,
            CHARGE_PER_LOAD_MINUTE_MIN,
            CHARGE_PER_LOAD_MINUTE_MAX),
        PowerRatings.fromJson(fields),
        PowerLoads.fromJson(fields),
        HumSettings.fromJson(fields));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_CELL_CAPACITY, cellCapacity);
    root.addProperty(KEY_GENERATOR_CAPACITY, generatorCapacity);
    root.addProperty(KEY_DUST_CHARGE, dustCharge);
    root.addProperty(KEY_LIGHTNING_CHARGE, lightningCharge);
    root.addProperty(KEY_CHARGE_PER_LOAD_MINUTE, chargePerLoadMinute);
    ratings.writeJson(root);
    loads.writeJson(root);
    hum.writeJson(root);
  }
}
