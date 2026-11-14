package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record LineSettings(int poleSpanBlocks, int spanBlocksPerWire) {

  public static final int SPAN_BLOCKS_MIN = 8;
  public static final int SPAN_BLOCKS_MAX = 64;
  public static final int BLOCKS_PER_WIRE_MIN = 1;
  public static final int BLOCKS_PER_WIRE_MAX = 32;

  public static final LineSettings DEFAULTS = new LineSettings(32, 4);

  static final String KEY_POLE_SPAN_BLOCKS = "poleSpanBlocks";
  static final String KEY_SPAN_BLOCKS_PER_WIRE = "spanBlocksPerWire";

  public LineSettings {
    poleSpanBlocks = ConfigBounds.clamp(poleSpanBlocks, SPAN_BLOCKS_MIN, SPAN_BLOCKS_MAX);
    spanBlocksPerWire =
        ConfigBounds.clamp(spanBlocksPerWire, BLOCKS_PER_WIRE_MIN, BLOCKS_PER_WIRE_MAX);
  }

  public LineSettings withPoleSpanBlocks(final int value) {
    return new LineSettings(value, spanBlocksPerWire);
  }

  public LineSettings withSpanBlocksPerWire(final int value) {
    return new LineSettings(poleSpanBlocks, value);
  }

  static LineSettings fromJson(final ConfigJsonFields fields) {
    return new LineSettings(
        fields.readInt(
            KEY_POLE_SPAN_BLOCKS, DEFAULTS.poleSpanBlocks, SPAN_BLOCKS_MIN, SPAN_BLOCKS_MAX),
        fields.readInt(
            KEY_SPAN_BLOCKS_PER_WIRE,
            DEFAULTS.spanBlocksPerWire,
            BLOCKS_PER_WIRE_MIN,
            BLOCKS_PER_WIRE_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_POLE_SPAN_BLOCKS, poleSpanBlocks);
    root.addProperty(KEY_SPAN_BLOCKS_PER_WIRE, spanBlocksPerWire);
  }
}
