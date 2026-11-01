package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record ClientWireSettings(
    int throughBlocksPercent, int onSurfacePercent, boolean showMarkers, boolean colouredLight) {

  public static final int THROUGH_MIN = 10;
  public static final int THROUGH_MAX = 100;
  public static final int SURFACE_MIN = 30;
  public static final int SURFACE_MAX = 100;
  public static final ClientWireSettings DEFAULTS = new ClientWireSettings(44, 100, true, true);

  static final String KEY_THROUGH = "throughBlocksPercent";
  static final String KEY_SURFACE = "onSurfacePercent";
  static final String KEY_MARKERS = "showMarkers";
  static final String KEY_COLOURED_LIGHT = "colouredLight";

  private static final int ALPHA_SHIFT = 24;
  private static final int FULL_ALPHA = 255;

  public ClientWireSettings {
    throughBlocksPercent = ConfigBounds.clamp(throughBlocksPercent, THROUGH_MIN, THROUGH_MAX);
    onSurfacePercent = ConfigBounds.clamp(onSurfacePercent, SURFACE_MIN, SURFACE_MAX);
  }

  public ClientWireSettings withThroughBlocksPercent(final int value) {
    return new ClientWireSettings(value, onSurfacePercent, showMarkers, colouredLight);
  }

  public ClientWireSettings withOnSurfacePercent(final int value) {
    return new ClientWireSettings(throughBlocksPercent, value, showMarkers, colouredLight);
  }

  public ClientWireSettings withShowMarkers(final boolean value) {
    return new ClientWireSettings(throughBlocksPercent, onSurfacePercent, value, colouredLight);
  }

  public ClientWireSettings withColouredLight(final boolean value) {
    return new ClientWireSettings(throughBlocksPercent, onSurfacePercent, showMarkers, value);
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

  static ClientWireSettings fromJson(final JsonObject root) {
    final ConfigJsonFields fields = new ConfigJsonFields(root);
    return new ClientWireSettings(
        fields.readInt(KEY_THROUGH, DEFAULTS.throughBlocksPercent, THROUGH_MIN, THROUGH_MAX),
        fields.readInt(KEY_SURFACE, DEFAULTS.onSurfacePercent, SURFACE_MIN, SURFACE_MAX),
        readBoolean(root, KEY_MARKERS, DEFAULTS.showMarkers),
        readBoolean(root, KEY_COLOURED_LIGHT, DEFAULTS.colouredLight));
  }

  private static boolean readBoolean(
      final JsonObject root, final String key, final boolean fallback) {
    return root.has(key)
            && root.get(key).isJsonPrimitive()
            && root.getAsJsonPrimitive(key).isBoolean()
        ? root.get(key).getAsBoolean()
        : fallback;
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
