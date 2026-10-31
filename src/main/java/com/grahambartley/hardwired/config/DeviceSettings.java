package com.grahambartley.hardwired.config;

import com.google.gson.JsonObject;

public record DeviceSettings(int motionSensorRangeBlocks, int buttonHoldTicks) {

  public static final int MOTION_RANGE_MIN = 1;
  public static final int MOTION_RANGE_MAX = 16;
  public static final int BUTTON_HOLD_MIN = 2;
  public static final int BUTTON_HOLD_MAX = 200;

  public static final DeviceSettings DEFAULTS = new DeviceSettings(6, 20);

  static final String KEY_MOTION_SENSOR_RANGE_BLOCKS = "motionSensorRangeBlocks";
  static final String KEY_BUTTON_HOLD_TICKS = "buttonHoldTicks";

  public DeviceSettings {
    motionSensorRangeBlocks =
        ConfigBounds.clamp(motionSensorRangeBlocks, MOTION_RANGE_MIN, MOTION_RANGE_MAX);
    buttonHoldTicks = ConfigBounds.clamp(buttonHoldTicks, BUTTON_HOLD_MIN, BUTTON_HOLD_MAX);
  }

  public DeviceSettings withMotionSensorRangeBlocks(final int value) {
    return new DeviceSettings(value, buttonHoldTicks);
  }

  public DeviceSettings withButtonHoldTicks(final int value) {
    return new DeviceSettings(motionSensorRangeBlocks, value);
  }

  static DeviceSettings fromJson(final ConfigJsonFields fields) {
    return new DeviceSettings(
        fields.readInt(
            KEY_MOTION_SENSOR_RANGE_BLOCKS,
            DEFAULTS.motionSensorRangeBlocks,
            MOTION_RANGE_MIN,
            MOTION_RANGE_MAX),
        fields.readInt(
            KEY_BUTTON_HOLD_TICKS, DEFAULTS.buttonHoldTicks, BUTTON_HOLD_MIN, BUTTON_HOLD_MAX));
  }

  void writeJson(final JsonObject root) {
    root.addProperty(KEY_MOTION_SENSOR_RANGE_BLOCKS, motionSensorRangeBlocks);
    root.addProperty(KEY_BUTTON_HOLD_TICKS, buttonHoldTicks);
  }
}
