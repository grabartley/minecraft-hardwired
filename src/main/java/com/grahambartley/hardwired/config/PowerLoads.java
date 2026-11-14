package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PowerLoads(
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
    int telephoneLoad,
    int exchangeLoad,
    int bellBoxLoad) {

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
  public static final int TELEPHONE_LOAD_MIN = 0;
  public static final int TELEPHONE_LOAD_MAX = 256;
  public static final int EXCHANGE_LOAD_MIN = 0;
  public static final int EXCHANGE_LOAD_MAX = 256;
  public static final int BELL_BOX_LOAD_MIN = 0;
  public static final int BELL_BOX_LOAD_MAX = 256;

  public static final PowerLoads DEFAULTS = new PowerLoads(1, 2, 2, 2, 4, 4, 4, 8, 16, 32, 1, 4, 1);

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
  static final String KEY_TELEPHONE_LOAD = "telephoneLoad";
  static final String KEY_EXCHANGE_LOAD = "exchangeLoad";
  static final String KEY_BELL_BOX_LOAD = "bellBoxLoad";

  public PowerLoads {
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
    telephoneLoad = ConfigBounds.clamp(telephoneLoad, TELEPHONE_LOAD_MIN, TELEPHONE_LOAD_MAX);
    exchangeLoad = ConfigBounds.clamp(exchangeLoad, EXCHANGE_LOAD_MIN, EXCHANGE_LOAD_MAX);
    bellBoxLoad = ConfigBounds.clamp(bellBoxLoad, BELL_BOX_LOAD_MIN, BELL_BOX_LOAD_MAX);
  }

  public PowerLoads withLampLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withStreetlightLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withLogicLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withVanillaLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withRadioLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withSpeakerLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withIntercomLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withAlarmLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withBroadcastLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withLiftLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withTelephoneLoad(final int value) {
    return new PowerLoads(
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
        exchangeLoad,
        bellBoxLoad);
  }

  public PowerLoads withExchangeLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        value,
        bellBoxLoad);
  }

  public PowerLoads withBellBoxLoad(final int value) {
    return new PowerLoads(
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
        telephoneLoad,
        exchangeLoad,
        value);
  }

  static PowerLoads fromJson(final ConfigJsonFields fields) {
    return new PowerLoads(
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
        fields.readInt(
            KEY_TELEPHONE_LOAD, DEFAULTS.telephoneLoad, TELEPHONE_LOAD_MIN, TELEPHONE_LOAD_MAX),
        fields.readInt(
            KEY_EXCHANGE_LOAD, DEFAULTS.exchangeLoad, EXCHANGE_LOAD_MIN, EXCHANGE_LOAD_MAX),
        fields.readInt(
            KEY_BELL_BOX_LOAD, DEFAULTS.bellBoxLoad, BELL_BOX_LOAD_MIN, BELL_BOX_LOAD_MAX));
  }

  void writeJson(final JsonObject root) {
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
    root.addProperty(KEY_TELEPHONE_LOAD, telephoneLoad);
    root.addProperty(KEY_EXCHANGE_LOAD, exchangeLoad);
    root.addProperty(KEY_BELL_BOX_LOAD, bellBoxLoad);
  }
}
