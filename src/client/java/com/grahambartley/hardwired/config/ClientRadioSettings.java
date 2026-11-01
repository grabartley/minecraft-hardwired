package com.grahambartley.hardwired.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record ClientRadioSettings(
    String microphone, boolean hearVoiceBroadcasts, int radioNoisePercent, int electricHumPercent) {

  public static final String DEFAULT_MICROPHONE = "";
  public static final int NOISE_MIN = 0;
  public static final int NOISE_MAX = 100;
  public static final ClientRadioSettings DEFAULTS =
      new ClientRadioSettings(DEFAULT_MICROPHONE, true, NOISE_MAX, NOISE_MAX);

  static final String KEY_MICROPHONE = "microphone";
  static final String KEY_HEAR_VOICE = "hearVoiceBroadcasts";
  static final String KEY_NOISE = "radioNoise";
  static final String KEY_HUM = "electricHum";

  public ClientRadioSettings {
    microphone = microphone == null ? DEFAULT_MICROPHONE : microphone;
    radioNoisePercent = ConfigBounds.clamp(radioNoisePercent, NOISE_MIN, NOISE_MAX);
    electricHumPercent = ConfigBounds.clamp(electricHumPercent, NOISE_MIN, NOISE_MAX);
  }

  public ClientRadioSettings withMicrophone(final String value) {
    return new ClientRadioSettings(
        value, hearVoiceBroadcasts, radioNoisePercent, electricHumPercent);
  }

  public ClientRadioSettings withHearVoiceBroadcasts(final boolean value) {
    return new ClientRadioSettings(microphone, value, radioNoisePercent, electricHumPercent);
  }

  public ClientRadioSettings withRadioNoisePercent(final int value) {
    return new ClientRadioSettings(microphone, hearVoiceBroadcasts, value, electricHumPercent);
  }

  public ClientRadioSettings withElectricHumPercent(final int value) {
    return new ClientRadioSettings(microphone, hearVoiceBroadcasts, radioNoisePercent, value);
  }

  static ClientRadioSettings fromJson(final JsonObject root) {
    final ConfigJsonFields fields = new ConfigJsonFields(root);
    final JsonElement name = root.get(KEY_MICROPHONE);
    final boolean named =
        name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString();
    return new ClientRadioSettings(
        named ? name.getAsString() : DEFAULTS.microphone,
        fields.readBoolean(KEY_HEAR_VOICE, DEFAULTS.hearVoiceBroadcasts),
        fields.readInt(KEY_NOISE, DEFAULTS.radioNoisePercent, NOISE_MIN, NOISE_MAX),
        fields.readInt(KEY_HUM, DEFAULTS.electricHumPercent, NOISE_MIN, NOISE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_MICROPHONE, microphone);
    root.addProperty(KEY_HEAR_VOICE, hearVoiceBroadcasts);
    root.addProperty(KEY_NOISE, radioNoisePercent);
    root.addProperty(KEY_HUM, electricHumPercent);
  }
}
