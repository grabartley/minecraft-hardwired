package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record PhoneSettings(
    int phoneRingSeconds, int exchangeLines, int bellBoxHearing, boolean voiceCalls) {

  public static final int RING_SECONDS_MIN = 5;
  public static final int RING_SECONDS_MAX = 300;
  public static final int EXCHANGE_LINES_MIN = 1;
  public static final int EXCHANGE_LINES_MAX = 12;
  public static final int BELL_BOX_HEARING_MIN = 1;
  public static final int BELL_BOX_HEARING_MAX = 256;

  public static final PhoneSettings DEFAULTS = new PhoneSettings(30, 4, 32, true);

  static final String KEY_PHONE_RING_SECONDS = "phoneRingSeconds";
  static final String KEY_EXCHANGE_LINES = "exchangeLines";
  static final String KEY_BELL_BOX_HEARING = "bellBoxHearing";
  static final String KEY_VOICE_CALLS = "voiceCalls";

  public PhoneSettings {
    phoneRingSeconds = ConfigBounds.clamp(phoneRingSeconds, RING_SECONDS_MIN, RING_SECONDS_MAX);
    exchangeLines = ConfigBounds.clamp(exchangeLines, EXCHANGE_LINES_MIN, EXCHANGE_LINES_MAX);
    bellBoxHearing = ConfigBounds.clamp(bellBoxHearing, BELL_BOX_HEARING_MIN, BELL_BOX_HEARING_MAX);
  }

  public PhoneSettings withPhoneRingSeconds(final int value) {
    return new PhoneSettings(value, exchangeLines, bellBoxHearing, voiceCalls);
  }

  public PhoneSettings withExchangeLines(final int value) {
    return new PhoneSettings(phoneRingSeconds, value, bellBoxHearing, voiceCalls);
  }

  public PhoneSettings withBellBoxHearing(final int value) {
    return new PhoneSettings(phoneRingSeconds, exchangeLines, value, voiceCalls);
  }

  public PhoneSettings withVoiceCalls(final boolean value) {
    return new PhoneSettings(phoneRingSeconds, exchangeLines, bellBoxHearing, value);
  }

  static PhoneSettings fromJson(final ConfigJsonFields fields) {
    return new PhoneSettings(
        fields.readInt(
            KEY_PHONE_RING_SECONDS, DEFAULTS.phoneRingSeconds, RING_SECONDS_MIN, RING_SECONDS_MAX),
        fields.readInt(
            KEY_EXCHANGE_LINES, DEFAULTS.exchangeLines, EXCHANGE_LINES_MIN, EXCHANGE_LINES_MAX),
        fields.readInt(
            KEY_BELL_BOX_HEARING,
            DEFAULTS.bellBoxHearing,
            BELL_BOX_HEARING_MIN,
            BELL_BOX_HEARING_MAX),
        fields.readBoolean(KEY_VOICE_CALLS, DEFAULTS.voiceCalls));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_PHONE_RING_SECONDS, phoneRingSeconds);
    root.addProperty(KEY_EXCHANGE_LINES, exchangeLines);
    root.addProperty(KEY_BELL_BOX_HEARING, bellBoxHearing);
    root.addProperty(KEY_VOICE_CALLS, voiceCalls);
  }
}
