package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record LiftSettings(int ticksPerBlock, int chargePerBlock) {

  public static final int TICKS_PER_BLOCK_MIN = 2;
  public static final int TICKS_PER_BLOCK_MAX = 40;
  public static final int CHARGE_PER_BLOCK_MIN = 0;
  public static final int CHARGE_PER_BLOCK_MAX = 100;

  public static final LiftSettings DEFAULTS = new LiftSettings(6, 2);

  static final String KEY_TICKS_PER_BLOCK = "ticksPerBlock";
  static final String KEY_CHARGE_PER_BLOCK = "chargePerBlock";

  public LiftSettings {
    ticksPerBlock = ConfigBounds.clamp(ticksPerBlock, TICKS_PER_BLOCK_MIN, TICKS_PER_BLOCK_MAX);
    chargePerBlock = ConfigBounds.clamp(chargePerBlock, CHARGE_PER_BLOCK_MIN, CHARGE_PER_BLOCK_MAX);
  }

  public LiftSettings withTicksPerBlock(final int value) {
    return new LiftSettings(value, chargePerBlock);
  }

  public LiftSettings withChargePerBlock(final int value) {
    return new LiftSettings(ticksPerBlock, value);
  }

  static LiftSettings fromJson(final ConfigJsonFields fields) {
    return new LiftSettings(
        fields.readInt(
            KEY_TICKS_PER_BLOCK, DEFAULTS.ticksPerBlock, TICKS_PER_BLOCK_MIN, TICKS_PER_BLOCK_MAX),
        fields.readInt(
            KEY_CHARGE_PER_BLOCK,
            DEFAULTS.chargePerBlock,
            CHARGE_PER_BLOCK_MIN,
            CHARGE_PER_BLOCK_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_TICKS_PER_BLOCK, ticksPerBlock);
    root.addProperty(KEY_CHARGE_PER_BLOCK, chargePerBlock);
  }
}
