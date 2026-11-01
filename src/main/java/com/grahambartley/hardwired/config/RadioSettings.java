package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record RadioSettings(
    int towerMinMasts,
    int towerMaxMasts,
    int towerBaseReach,
    int towerReachPerMast,
    int radioHearing,
    int speakerHearing,
    int undergroundFreeDepth,
    int undergroundLossDepth) {

  public static final int TOWER_MIN_MASTS_MIN = 1;
  public static final int TOWER_MIN_MASTS_MAX = 64;
  public static final int TOWER_MAX_MASTS_MIN = 1;
  public static final int TOWER_MAX_MASTS_MAX = 128;
  public static final int TOWER_BASE_REACH_MIN = 0;
  public static final int TOWER_BASE_REACH_MAX = 4096;
  public static final int TOWER_REACH_PER_MAST_MIN = 0;
  public static final int TOWER_REACH_PER_MAST_MAX = 512;
  public static final int RADIO_HEARING_MIN = 1;
  public static final int RADIO_HEARING_MAX = 128;
  public static final int SPEAKER_HEARING_MIN = 1;
  public static final int SPEAKER_HEARING_MAX = 256;
  public static final int UNDERGROUND_FREE_DEPTH_MIN = 0;
  public static final int UNDERGROUND_FREE_DEPTH_MAX = 256;
  public static final int UNDERGROUND_LOSS_DEPTH_MIN = 1;
  public static final int UNDERGROUND_LOSS_DEPTH_MAX = 256;

  public static final RadioSettings DEFAULTS = new RadioSettings(5, 32, 64, 32, 24, 64, 8, 40);

  static final String KEY_TOWER_MIN_MASTS = "towerMinMasts";
  static final String KEY_TOWER_MAX_MASTS = "towerMaxMasts";
  static final String KEY_TOWER_BASE_REACH = "towerBaseReach";
  static final String KEY_TOWER_REACH_PER_MAST = "towerReachPerMast";
  static final String KEY_RADIO_HEARING = "radioHearing";
  static final String KEY_SPEAKER_HEARING = "speakerHearing";
  static final String KEY_UNDERGROUND_FREE_DEPTH = "undergroundFreeDepth";
  static final String KEY_UNDERGROUND_LOSS_DEPTH = "undergroundLossDepth";

  public RadioSettings {
    towerMinMasts = ConfigBounds.clamp(towerMinMasts, TOWER_MIN_MASTS_MIN, TOWER_MIN_MASTS_MAX);
    towerMaxMasts = ConfigBounds.clamp(towerMaxMasts, TOWER_MAX_MASTS_MIN, TOWER_MAX_MASTS_MAX);
    towerBaseReach = ConfigBounds.clamp(towerBaseReach, TOWER_BASE_REACH_MIN, TOWER_BASE_REACH_MAX);
    towerReachPerMast =
        ConfigBounds.clamp(towerReachPerMast, TOWER_REACH_PER_MAST_MIN, TOWER_REACH_PER_MAST_MAX);
    radioHearing = ConfigBounds.clamp(radioHearing, RADIO_HEARING_MIN, RADIO_HEARING_MAX);
    speakerHearing = ConfigBounds.clamp(speakerHearing, SPEAKER_HEARING_MIN, SPEAKER_HEARING_MAX);
    undergroundFreeDepth =
        ConfigBounds.clamp(
            undergroundFreeDepth, UNDERGROUND_FREE_DEPTH_MIN, UNDERGROUND_FREE_DEPTH_MAX);
    undergroundLossDepth =
        ConfigBounds.clamp(
            undergroundLossDepth, UNDERGROUND_LOSS_DEPTH_MIN, UNDERGROUND_LOSS_DEPTH_MAX);
  }

  public RadioSettings withTowerMinMasts(final int value) {
    return new RadioSettings(
        value,
        towerMaxMasts,
        towerBaseReach,
        towerReachPerMast,
        radioHearing,
        speakerHearing,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withTowerMaxMasts(final int value) {
    return new RadioSettings(
        towerMinMasts,
        value,
        towerBaseReach,
        towerReachPerMast,
        radioHearing,
        speakerHearing,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withTowerBaseReach(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        value,
        towerReachPerMast,
        radioHearing,
        speakerHearing,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withTowerReachPerMast(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        towerBaseReach,
        value,
        radioHearing,
        speakerHearing,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withRadioHearing(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        towerBaseReach,
        towerReachPerMast,
        value,
        speakerHearing,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withSpeakerHearing(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        towerBaseReach,
        towerReachPerMast,
        radioHearing,
        value,
        undergroundFreeDepth,
        undergroundLossDepth);
  }

  public RadioSettings withUndergroundFreeDepth(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        towerBaseReach,
        towerReachPerMast,
        radioHearing,
        speakerHearing,
        value,
        undergroundLossDepth);
  }

  public RadioSettings withUndergroundLossDepth(final int value) {
    return new RadioSettings(
        towerMinMasts,
        towerMaxMasts,
        towerBaseReach,
        towerReachPerMast,
        radioHearing,
        speakerHearing,
        undergroundFreeDepth,
        value);
  }

  static RadioSettings fromJson(final ConfigJsonFields fields) {
    return new RadioSettings(
        fields.readInt(
            KEY_TOWER_MIN_MASTS, DEFAULTS.towerMinMasts, TOWER_MIN_MASTS_MIN, TOWER_MIN_MASTS_MAX),
        fields.readInt(
            KEY_TOWER_MAX_MASTS, DEFAULTS.towerMaxMasts, TOWER_MAX_MASTS_MIN, TOWER_MAX_MASTS_MAX),
        fields.readInt(
            KEY_TOWER_BASE_REACH,
            DEFAULTS.towerBaseReach,
            TOWER_BASE_REACH_MIN,
            TOWER_BASE_REACH_MAX),
        fields.readInt(
            KEY_TOWER_REACH_PER_MAST,
            DEFAULTS.towerReachPerMast,
            TOWER_REACH_PER_MAST_MIN,
            TOWER_REACH_PER_MAST_MAX),
        fields.readInt(
            KEY_RADIO_HEARING, DEFAULTS.radioHearing, RADIO_HEARING_MIN, RADIO_HEARING_MAX),
        fields.readInt(
            KEY_SPEAKER_HEARING, DEFAULTS.speakerHearing, SPEAKER_HEARING_MIN, SPEAKER_HEARING_MAX),
        fields.readInt(
            KEY_UNDERGROUND_FREE_DEPTH,
            DEFAULTS.undergroundFreeDepth,
            UNDERGROUND_FREE_DEPTH_MIN,
            UNDERGROUND_FREE_DEPTH_MAX),
        fields.readInt(
            KEY_UNDERGROUND_LOSS_DEPTH,
            DEFAULTS.undergroundLossDepth,
            UNDERGROUND_LOSS_DEPTH_MIN,
            UNDERGROUND_LOSS_DEPTH_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_TOWER_MIN_MASTS, towerMinMasts);
    root.addProperty(KEY_TOWER_MAX_MASTS, towerMaxMasts);
    root.addProperty(KEY_TOWER_BASE_REACH, towerBaseReach);
    root.addProperty(KEY_TOWER_REACH_PER_MAST, towerReachPerMast);
    root.addProperty(KEY_RADIO_HEARING, radioHearing);
    root.addProperty(KEY_SPEAKER_HEARING, speakerHearing);
    root.addProperty(KEY_UNDERGROUND_FREE_DEPTH, undergroundFreeDepth);
    root.addProperty(KEY_UNDERGROUND_LOSS_DEPTH, undergroundLossDepth);
  }
}
