package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record ClientDisplaySettings(
    int throughBlocksPercent, int onSurfacePercent, boolean showMarkers, boolean colouredLight) {

  public static final int THROUGH_MIN = 10;
  public static final int THROUGH_MAX = 100;
  public static final int SURFACE_MIN = 30;
  public static final int SURFACE_MAX = 100;
  public static final ClientDisplaySettings DEFAULTS =
      new ClientDisplaySettings(44, 100, true, true);

  static final String KEY_THROUGH = "throughBlocksPercent";
  static final String KEY_SURFACE = "onSurfacePercent";
  static final String KEY_MARKERS = "showMarkers";
  static final String KEY_COLOURED_LIGHT = "colouredLight";

  private static final int ALPHA_SHIFT = 24;
  private static final int FULL_ALPHA = 255;

  public ClientDisplaySettings {
    throughBlocksPercent = ConfigBounds.clamp(throughBlocksPercent, THROUGH_MIN, THROUGH_MAX);
    onSurfacePercent = ConfigBounds.clamp(onSurfacePercent, SURFACE_MIN, SURFACE_MAX);
  }

  public ClientDisplaySettings withThroughBlocksPercent(final int value) {
    return new ClientDisplaySettings(value, onSurfacePercent, showMarkers, colouredLight);
  }

  public ClientDisplaySettings withOnSurfacePercent(final int value) {
    return new ClientDisplaySettings(throughBlocksPercent, value, showMarkers, colouredLight);
  }

  public ClientDisplaySettings withShowMarkers(final boolean value) {
    return new ClientDisplaySettings(throughBlocksPercent, onSurfacePercent, value, colouredLight);
  }

  public ClientDisplaySettings withColouredLight(final boolean value) {
    return new ClientDisplaySettings(throughBlocksPercent, onSurfacePercent, showMarkers, value);
  }

  public int throughBlocksAlpha() {
    return alpha(throughBlocksPercent);
  }

  public int onSurfaceAlpha() {
    return alpha(onSurfacePercent);
  }

  private static int alpha(final int percent) {
    return (percent * FULL_ALPHA / 100) << ALPHA_SHIFT;
  }

  static ClientDisplaySettings fromJson(final JsonObject root) {
    final ConfigJsonFields fields = new ConfigJsonFields(root);
    return new ClientDisplaySettings(
        fields.readInt(KEY_THROUGH, DEFAULTS.throughBlocksPercent, THROUGH_MIN, THROUGH_MAX),
        fields.readInt(KEY_SURFACE, DEFAULTS.onSurfacePercent, SURFACE_MIN, SURFACE_MAX),
        fields.readBoolean(KEY_MARKERS, DEFAULTS.showMarkers),
        fields.readBoolean(KEY_COLOURED_LIGHT, DEFAULTS.colouredLight));
  }

  JsonObject toJson() {
    final JsonObject root = new JsonObject();
    root.addProperty(KEY_THROUGH, throughBlocksPercent);
    root.addProperty(KEY_SURFACE, onSurfacePercent);
    root.addProperty(KEY_MARKERS, showMarkers);
    root.addProperty(KEY_COLOURED_LIGHT, colouredLight);
    return root;
  }
}
