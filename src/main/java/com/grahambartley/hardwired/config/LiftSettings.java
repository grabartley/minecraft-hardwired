package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record LiftSettings(int ticksPerBlock) {

  public static final int TICKS_PER_BLOCK_MIN = 2;
  public static final int TICKS_PER_BLOCK_MAX = 40;

  public static final LiftSettings DEFAULTS = new LiftSettings(6);

  static final String KEY_TICKS_PER_BLOCK = "ticksPerBlock";

  public LiftSettings {
    ticksPerBlock = ConfigBounds.clamp(ticksPerBlock, TICKS_PER_BLOCK_MIN, TICKS_PER_BLOCK_MAX);
  }

  public LiftSettings withTicksPerBlock(final int value) {
    return new LiftSettings(value);
  }

  static LiftSettings fromJson(final ConfigJsonFields fields) {
    return new LiftSettings(
        fields.readInt(
            KEY_TICKS_PER_BLOCK, DEFAULTS.ticksPerBlock, TICKS_PER_BLOCK_MIN, TICKS_PER_BLOCK_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_TICKS_PER_BLOCK, ticksPerBlock);
  }
}
