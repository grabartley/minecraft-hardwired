package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PowerSettings(
    int cellCapacity,
    int generatorCapacity,
    int dustCharge,
    int lightningCharge,
    int chargePerLoadMinute,
    int cellRating,
    int generatorRating,
    int solarRating,
    int dynamoRating,
    int lampLoad,
    int streetlightLoad,
    int logicLoad,
    int vanillaLoad,
    int radioLoad,
    int speakerLoad,
    int intercomLoad,
    int alarmLoad,
    int broadcastLoad,
    int liftLoad,
    int humRange,
    int humStartLoad,
    int humFullLoad) {

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
  public static final int CELL_RATING_MIN = 0;
  public static final int CELL_RATING_MAX = 1024;
  public static final int GENERATOR_RATING_MIN = 0;
  public static final int GENERATOR_RATING_MAX = 1024;
  public static final int SOLAR_RATING_MIN = 0;
  public static final int SOLAR_RATING_MAX = 1024;
  public static final int DYNAMO_RATING_MIN = 0;
  public static final int DYNAMO_RATING_MAX = 1024;
  public static final int LAMP_LOAD_MIN = 0;
  public static final int LAMP_LOAD_MAX = 256;
  public static final int STREETLIGHT_LOAD_MIN = 0;
  public static final int STREETLIGHT_LOAD_MAX = 256;
  public static final int LOGIC_LOAD_MIN = 0;
  public static final int LOGIC_LOAD_MAX = 256;
  public static final int VANILLA_LOAD_MIN = 0;
  public static final int VANILLA_LOAD_MAX = 256;
  public static final int RADIO_LOAD_MIN = 0;
  public static final int RADIO_LOAD_MAX = 256;
  public static final int SPEAKER_LOAD_MIN = 0;
  public static final int SPEAKER_LOAD_MAX = 256;
  public static final int INTERCOM_LOAD_MIN = 0;
  public static final int INTERCOM_LOAD_MAX = 256;
  public static final int ALARM_LOAD_MIN = 0;
  public static final int ALARM_LOAD_MAX = 256;
  public static final int BROADCAST_LOAD_MIN = 0;
  public static final int BROADCAST_LOAD_MAX = 256;
  public static final int LIFT_LOAD_MIN = 0;
  public static final int LIFT_LOAD_MAX = 256;
  public static final int HUM_RANGE_MIN = 4;
  public static final int HUM_RANGE_MAX = 64;
  public static final int HUM_START_LOAD_MIN = 0;
  public static final int HUM_START_LOAD_MAX = 4096;
  public static final int HUM_FULL_LOAD_MIN = 1;
  public static final int HUM_FULL_LOAD_MAX = 4096;

  public static final PowerSettings DEFAULTS =
      new PowerSettings(
          1000, 4000, 10, 500, 3, 16, 64, 6, 24, 1, 2, 2, 2, 4, 4, 4, 8, 16, 32, 16, 24, 128);

  static final String KEY_CELL_CAPACITY = "cellCapacity";
  static final String KEY_GENERATOR_CAPACITY = "generatorCapacity";
  static final String KEY_DUST_CHARGE = "dustCharge";
  static final String KEY_LIGHTNING_CHARGE = "lightningCharge";
  static final String KEY_CHARGE_PER_LOAD_MINUTE = "chargePerLoadMinute";
  static final String KEY_CELL_RATING = "cellRating";
  static final String KEY_GENERATOR_RATING = "generatorRating";
  static final String KEY_SOLAR_RATING = "solarRating";
  static final String KEY_DYNAMO_RATING = "dynamoRating";
  static final String KEY_LAMP_LOAD = "lampLoad";
  static final String KEY_STREETLIGHT_LOAD = "streetlightLoad";
  static final String KEY_LOGIC_LOAD = "logicLoad";
  static final String KEY_VANILLA_LOAD = "vanillaLoad";
  static final String KEY_RADIO_LOAD = "radioLoad";
  static final String KEY_SPEAKER_LOAD = "speakerLoad";
  static final String KEY_INTERCOM_LOAD = "intercomLoad";
  static final String KEY_ALARM_LOAD = "alarmLoad";
  static final String KEY_BROADCAST_LOAD = "broadcastLoad";
  static final String KEY_LIFT_LOAD = "liftLoad";
  static final String KEY_HUM_RANGE = "humRange";
  static final String KEY_HUM_START_LOAD = "humStartLoad";
  static final String KEY_HUM_FULL_LOAD = "humFullLoad";

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
    cellRating = ConfigBounds.clamp(cellRating, CELL_RATING_MIN, CELL_RATING_MAX);
    generatorRating =
        ConfigBounds.clamp(generatorRating, GENERATOR_RATING_MIN, GENERATOR_RATING_MAX);
    solarRating = ConfigBounds.clamp(solarRating, SOLAR_RATING_MIN, SOLAR_RATING_MAX);
    dynamoRating = ConfigBounds.clamp(dynamoRating, DYNAMO_RATING_MIN, DYNAMO_RATING_MAX);
    lampLoad = ConfigBounds.clamp(lampLoad, LAMP_LOAD_MIN, LAMP_LOAD_MAX);
    streetlightLoad =
        ConfigBounds.clamp(streetlightLoad, STREETLIGHT_LOAD_MIN, STREETLIGHT_LOAD_MAX);
    logicLoad = ConfigBounds.clamp(logicLoad, LOGIC_LOAD_MIN, LOGIC_LOAD_MAX);
    vanillaLoad = ConfigBounds.clamp(vanillaLoad, VANILLA_LOAD_MIN, VANILLA_LOAD_MAX);
    radioLoad = ConfigBounds.clamp(radioLoad, RADIO_LOAD_MIN, RADIO_LOAD_MAX);
    speakerLoad = ConfigBounds.clamp(speakerLoad, SPEAKER_LOAD_MIN, SPEAKER_LOAD_MAX);
    intercomLoad = ConfigBounds.clamp(intercomLoad, INTERCOM_LOAD_MIN, INTERCOM_LOAD_MAX);
    alarmLoad = ConfigBounds.clamp(alarmLoad, ALARM_LOAD_MIN, ALARM_LOAD_MAX);
    broadcastLoad = ConfigBounds.clamp(broadcastLoad, BROADCAST_LOAD_MIN, BROADCAST_LOAD_MAX);
    liftLoad = ConfigBounds.clamp(liftLoad, LIFT_LOAD_MIN, LIFT_LOAD_MAX);
    humRange = ConfigBounds.clamp(humRange, HUM_RANGE_MIN, HUM_RANGE_MAX);
    humStartLoad = ConfigBounds.clamp(humStartLoad, HUM_START_LOAD_MIN, HUM_START_LOAD_MAX);
    humFullLoad = ConfigBounds.clamp(humFullLoad, HUM_FULL_LOAD_MIN, HUM_FULL_LOAD_MAX);
  }

  public PowerSettings withCellCapacity(final int value) {
    return new PowerSettings(
        value,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withGeneratorCapacity(final int value) {
    return new PowerSettings(
        cellCapacity,
        value,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withDustCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        value,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withLightningCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        value,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withChargePerLoadMinute(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        value,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withCellRating(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        value,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withGeneratorRating(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        value,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withSolarRating(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        value,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withDynamoRating(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        value,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withLampLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        value,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withStreetlightLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        value,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withLogicLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        value,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withVanillaLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        value,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withRadioLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        value,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withSpeakerLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        value,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withIntercomLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        value,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withAlarmLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        value,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withBroadcastLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        value,
        liftLoad,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withLiftLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        value,
        humRange,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withHumRange(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        value,
        humStartLoad,
        humFullLoad);
  }

  public PowerSettings withHumStartLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        value,
        humFullLoad);
  }

  public PowerSettings withHumFullLoad(final int value) {
    return new PowerSettings(
        cellCapacity,
        generatorCapacity,
        dustCharge,
        lightningCharge,
        chargePerLoadMinute,
        cellRating,
        generatorRating,
        solarRating,
        dynamoRating,
        lampLoad,
        streetlightLoad,
        logicLoad,
        vanillaLoad,
        radioLoad,
        speakerLoad,
        intercomLoad,
        alarmLoad,
        broadcastLoad,
        liftLoad,
        humRange,
        humStartLoad,
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
        fields.readInt(KEY_CELL_RATING, DEFAULTS.cellRating, CELL_RATING_MIN, CELL_RATING_MAX),
        fields.readInt(
            KEY_GENERATOR_RATING,
            DEFAULTS.generatorRating,
            GENERATOR_RATING_MIN,
            GENERATOR_RATING_MAX),
        fields.readInt(KEY_SOLAR_RATING, DEFAULTS.solarRating, SOLAR_RATING_MIN, SOLAR_RATING_MAX),
        fields.readInt(
            KEY_DYNAMO_RATING, DEFAULTS.dynamoRating, DYNAMO_RATING_MIN, DYNAMO_RATING_MAX),
        fields.readInt(KEY_LAMP_LOAD, DEFAULTS.lampLoad, LAMP_LOAD_MIN, LAMP_LOAD_MAX),
        fields.readInt(
            KEY_STREETLIGHT_LOAD,
            DEFAULTS.streetlightLoad,
            STREETLIGHT_LOAD_MIN,
            STREETLIGHT_LOAD_MAX),
        fields.readInt(KEY_LOGIC_LOAD, DEFAULTS.logicLoad, LOGIC_LOAD_MIN, LOGIC_LOAD_MAX),
        fields.readInt(KEY_VANILLA_LOAD, DEFAULTS.vanillaLoad, VANILLA_LOAD_MIN, VANILLA_LOAD_MAX),
        fields.readInt(KEY_RADIO_LOAD, DEFAULTS.radioLoad, RADIO_LOAD_MIN, RADIO_LOAD_MAX),
        fields.readInt(KEY_SPEAKER_LOAD, DEFAULTS.speakerLoad, SPEAKER_LOAD_MIN, SPEAKER_LOAD_MAX),
        fields.readInt(
            KEY_INTERCOM_LOAD, DEFAULTS.intercomLoad, INTERCOM_LOAD_MIN, INTERCOM_LOAD_MAX),
        fields.readInt(KEY_ALARM_LOAD, DEFAULTS.alarmLoad, ALARM_LOAD_MIN, ALARM_LOAD_MAX),
        fields.readInt(
            KEY_BROADCAST_LOAD, DEFAULTS.broadcastLoad, BROADCAST_LOAD_MIN, BROADCAST_LOAD_MAX),
        fields.readInt(KEY_LIFT_LOAD, DEFAULTS.liftLoad, LIFT_LOAD_MIN, LIFT_LOAD_MAX),
        fields.readInt(KEY_HUM_RANGE, DEFAULTS.humRange, HUM_RANGE_MIN, HUM_RANGE_MAX),
        fields.readInt(
            KEY_HUM_START_LOAD, DEFAULTS.humStartLoad, HUM_START_LOAD_MIN, HUM_START_LOAD_MAX),
        fields.readInt(
            KEY_HUM_FULL_LOAD, DEFAULTS.humFullLoad, HUM_FULL_LOAD_MIN, HUM_FULL_LOAD_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_CELL_CAPACITY, cellCapacity);
    root.addProperty(KEY_GENERATOR_CAPACITY, generatorCapacity);
    root.addProperty(KEY_DUST_CHARGE, dustCharge);
    root.addProperty(KEY_LIGHTNING_CHARGE, lightningCharge);
    root.addProperty(KEY_CHARGE_PER_LOAD_MINUTE, chargePerLoadMinute);
    root.addProperty(KEY_CELL_RATING, cellRating);
    root.addProperty(KEY_GENERATOR_RATING, generatorRating);
    root.addProperty(KEY_SOLAR_RATING, solarRating);
    root.addProperty(KEY_DYNAMO_RATING, dynamoRating);
    root.addProperty(KEY_LAMP_LOAD, lampLoad);
    root.addProperty(KEY_STREETLIGHT_LOAD, streetlightLoad);
    root.addProperty(KEY_LOGIC_LOAD, logicLoad);
    root.addProperty(KEY_VANILLA_LOAD, vanillaLoad);
    root.addProperty(KEY_RADIO_LOAD, radioLoad);
    root.addProperty(KEY_SPEAKER_LOAD, speakerLoad);
    root.addProperty(KEY_INTERCOM_LOAD, intercomLoad);
    root.addProperty(KEY_ALARM_LOAD, alarmLoad);
    root.addProperty(KEY_BROADCAST_LOAD, broadcastLoad);
    root.addProperty(KEY_LIFT_LOAD, liftLoad);
    root.addProperty(KEY_HUM_RANGE, humRange);
    root.addProperty(KEY_HUM_START_LOAD, humStartLoad);
    root.addProperty(KEY_HUM_FULL_LOAD, humFullLoad);
  }
}
