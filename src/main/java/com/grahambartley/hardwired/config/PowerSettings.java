package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PowerSettings(
    int cellCapacity,
    int dustCharge,
    int solarChargePerMinute,
    int lightningCharge,
    int alarmChargePerSecond,
    int radioChargePerSecond,
    int speakerChargePerSecond,
    int intercomChargePerLine) {

  public static final int CELL_CAPACITY_MIN = 100;
  public static final int CELL_CAPACITY_MAX = 100000;
  public static final int DUST_CHARGE_MIN = 1;
  public static final int DUST_CHARGE_MAX = 1000;
  public static final int SOLAR_CHARGE_MIN = 0;
  public static final int SOLAR_CHARGE_MAX = 600;
  public static final int LIGHTNING_CHARGE_MIN = 0;
  public static final int LIGHTNING_CHARGE_MAX = 100000;
  public static final int ALARM_CHARGE_MIN = 0;
  public static final int ALARM_CHARGE_MAX = 100;
  public static final int RADIO_CHARGE_MIN = 0;
  public static final int RADIO_CHARGE_MAX = 100;
  public static final int SPEAKER_CHARGE_MIN = 0;
  public static final int SPEAKER_CHARGE_MAX = 100;
  public static final int INTERCOM_CHARGE_MIN = 0;
  public static final int INTERCOM_CHARGE_MAX = 100;

  public static final PowerSettings DEFAULTS = new PowerSettings(1000, 10, 6, 500, 1, 1, 1, 1);

  static final String KEY_CELL_CAPACITY = "cellCapacity";
  static final String KEY_DUST_CHARGE = "dustCharge";
  static final String KEY_SOLAR_CHARGE = "solarChargePerMinute";
  static final String KEY_LIGHTNING_CHARGE = "lightningCharge";
  static final String KEY_ALARM_CHARGE = "alarmChargePerSecond";
  static final String KEY_RADIO_CHARGE = "radioChargePerSecond";
  static final String KEY_SPEAKER_CHARGE = "speakerChargePerSecond";
  static final String KEY_INTERCOM_CHARGE = "intercomChargePerLine";

  public PowerSettings {
    cellCapacity = ConfigBounds.clamp(cellCapacity, CELL_CAPACITY_MIN, CELL_CAPACITY_MAX);
    dustCharge = ConfigBounds.clamp(dustCharge, DUST_CHARGE_MIN, DUST_CHARGE_MAX);
    solarChargePerMinute =
        ConfigBounds.clamp(solarChargePerMinute, SOLAR_CHARGE_MIN, SOLAR_CHARGE_MAX);
    lightningCharge =
        ConfigBounds.clamp(lightningCharge, LIGHTNING_CHARGE_MIN, LIGHTNING_CHARGE_MAX);
    alarmChargePerSecond =
        ConfigBounds.clamp(alarmChargePerSecond, ALARM_CHARGE_MIN, ALARM_CHARGE_MAX);
    radioChargePerSecond =
        ConfigBounds.clamp(radioChargePerSecond, RADIO_CHARGE_MIN, RADIO_CHARGE_MAX);
    speakerChargePerSecond =
        ConfigBounds.clamp(speakerChargePerSecond, SPEAKER_CHARGE_MIN, SPEAKER_CHARGE_MAX);
    intercomChargePerLine =
        ConfigBounds.clamp(intercomChargePerLine, INTERCOM_CHARGE_MIN, INTERCOM_CHARGE_MAX);
  }

  public PowerSettings withCellCapacity(final int value) {
    return new PowerSettings(
        value,
        dustCharge,
        solarChargePerMinute,
        lightningCharge,
        alarmChargePerSecond,
        radioChargePerSecond,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withDustCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        value,
        solarChargePerMinute,
        lightningCharge,
        alarmChargePerSecond,
        radioChargePerSecond,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withSolarChargePerMinute(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        value,
        lightningCharge,
        alarmChargePerSecond,
        radioChargePerSecond,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withLightningCharge(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        solarChargePerMinute,
        value,
        alarmChargePerSecond,
        radioChargePerSecond,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withAlarmChargePerSecond(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        solarChargePerMinute,
        lightningCharge,
        value,
        radioChargePerSecond,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withRadioChargePerSecond(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        solarChargePerMinute,
        lightningCharge,
        alarmChargePerSecond,
        value,
        speakerChargePerSecond,
        intercomChargePerLine);
  }

  public PowerSettings withSpeakerChargePerSecond(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        solarChargePerMinute,
        lightningCharge,
        alarmChargePerSecond,
        radioChargePerSecond,
        value,
        intercomChargePerLine);
  }

  public PowerSettings withIntercomChargePerLine(final int value) {
    return new PowerSettings(
        cellCapacity,
        dustCharge,
        solarChargePerMinute,
        lightningCharge,
        alarmChargePerSecond,
        radioChargePerSecond,
        speakerChargePerSecond,
        value);
  }

  static PowerSettings fromJson(final ConfigJsonFields fields) {
    return new PowerSettings(
        fields.readInt(
            KEY_CELL_CAPACITY, DEFAULTS.cellCapacity, CELL_CAPACITY_MIN, CELL_CAPACITY_MAX),
        fields.readInt(KEY_DUST_CHARGE, DEFAULTS.dustCharge, DUST_CHARGE_MIN, DUST_CHARGE_MAX),
        fields.readInt(
            KEY_SOLAR_CHARGE, DEFAULTS.solarChargePerMinute, SOLAR_CHARGE_MIN, SOLAR_CHARGE_MAX),
        fields.readInt(
            KEY_LIGHTNING_CHARGE,
            DEFAULTS.lightningCharge,
            LIGHTNING_CHARGE_MIN,
            LIGHTNING_CHARGE_MAX),
        fields.readInt(
            KEY_ALARM_CHARGE, DEFAULTS.alarmChargePerSecond, ALARM_CHARGE_MIN, ALARM_CHARGE_MAX),
        fields.readInt(
            KEY_RADIO_CHARGE, DEFAULTS.radioChargePerSecond, RADIO_CHARGE_MIN, RADIO_CHARGE_MAX),
        fields.readInt(
            KEY_SPEAKER_CHARGE,
            DEFAULTS.speakerChargePerSecond,
            SPEAKER_CHARGE_MIN,
            SPEAKER_CHARGE_MAX),
        fields.readInt(
            KEY_INTERCOM_CHARGE,
            DEFAULTS.intercomChargePerLine,
            INTERCOM_CHARGE_MIN,
            INTERCOM_CHARGE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_CELL_CAPACITY, cellCapacity);
    root.addProperty(KEY_DUST_CHARGE, dustCharge);
    root.addProperty(KEY_SOLAR_CHARGE, solarChargePerMinute);
    root.addProperty(KEY_LIGHTNING_CHARGE, lightningCharge);
    root.addProperty(KEY_ALARM_CHARGE, alarmChargePerSecond);
    root.addProperty(KEY_RADIO_CHARGE, radioChargePerSecond);
    root.addProperty(KEY_SPEAKER_CHARGE, speakerChargePerSecond);
    root.addProperty(KEY_INTERCOM_CHARGE, intercomChargePerLine);
  }
}
