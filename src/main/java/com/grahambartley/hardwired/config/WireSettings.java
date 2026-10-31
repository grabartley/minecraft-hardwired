package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record WireSettings(int viewRangeBlocks, int maxNetworkSize) {

  public static final int VIEW_RANGE_MIN = 8;
  public static final int VIEW_RANGE_MAX = 96;
  public static final int NETWORK_SIZE_MIN = 64;
  public static final int NETWORK_SIZE_MAX = 65536;

  public static final WireSettings DEFAULTS = new WireSettings(32, 8192);

  static final String KEY_VIEW_RANGE_BLOCKS = "wireViewRangeBlocks";
  static final String KEY_MAX_NETWORK_SIZE = "maxNetworkSize";

  public WireSettings {
    viewRangeBlocks = ConfigBounds.clamp(viewRangeBlocks, VIEW_RANGE_MIN, VIEW_RANGE_MAX);
    maxNetworkSize = ConfigBounds.clamp(maxNetworkSize, NETWORK_SIZE_MIN, NETWORK_SIZE_MAX);
  }

  public WireSettings withViewRangeBlocks(final int value) {
    return new WireSettings(value, maxNetworkSize);
  }

  public WireSettings withMaxNetworkSize(final int value) {
    return new WireSettings(viewRangeBlocks, value);
  }

  static WireSettings fromJson(final ConfigJsonFields fields) {
    return new WireSettings(
        fields.readInt(
            KEY_VIEW_RANGE_BLOCKS, DEFAULTS.viewRangeBlocks, VIEW_RANGE_MIN, VIEW_RANGE_MAX),
        fields.readInt(
            KEY_MAX_NETWORK_SIZE, DEFAULTS.maxNetworkSize, NETWORK_SIZE_MIN, NETWORK_SIZE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_VIEW_RANGE_BLOCKS, viewRangeBlocks);
    root.addProperty(KEY_MAX_NETWORK_SIZE, maxNetworkSize);
  }
}
