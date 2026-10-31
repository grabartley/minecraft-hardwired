package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.WireSettings;

final class SettingsConfigSection {
  private static final String KEY = "screen.hardwired.settings.";

  private HardwiredConfig config;

  SettingsConfigSection(final HardwiredConfig config) {
    this.config = config;
  }

  void addTo(final HardwiredConfigList list, final ConfigRows rows) {
    list.addRow(rows.header(KEY + "wire"));
    list.addRow(
        rows.intSlider(
            KEY + "wireviewrange",
            KEY + "wireviewrange.tooltip",
            WireSettings.VIEW_RANGE_MIN,
            WireSettings.VIEW_RANGE_MAX,
            config.wire().viewRangeBlocks(),
            value -> config = config.withWire(config.wire().withViewRangeBlocks(value))));
    list.addRow(
        rows.intSlider(
            KEY + "maxnetworksize",
            KEY + "maxnetworksize.tooltip",
            WireSettings.NETWORK_SIZE_MIN,
            WireSettings.NETWORK_SIZE_MAX,
            config.wire().maxNetworkSize(),
            value -> config = config.withWire(config.wire().withMaxNetworkSize(value))));
    list.addRow(rows.header(KEY + "lamps"));
    list.addRow(
        rows.percentSlider(
            KEY + "lampagerate",
            LampSettings.AGE_RATE_MIN,
            LampSettings.AGE_RATE_MAX,
            config.lamp().ageRateMultiplierPercent(),
            value -> config = config.withLamp(config.lamp().withAgeRateMultiplierPercent(value))));
    list.addRow(rows.header(KEY + "devices"));
    list.addRow(
        rows.intSlider(
            KEY + "motionsensorrange",
            KEY + "motionsensorrange.tooltip",
            DeviceSettings.MOTION_RANGE_MIN,
            DeviceSettings.MOTION_RANGE_MAX,
            config.device().motionSensorRangeBlocks(),
            value ->
                config = config.withDevice(config.device().withMotionSensorRangeBlocks(value))));
    list.addRow(
        rows.intSlider(
            KEY + "buttonholdticks",
            KEY + "buttonholdticks.tooltip",
            DeviceSettings.BUTTON_HOLD_MIN,
            DeviceSettings.BUTTON_HOLD_MAX,
            config.device().buttonHoldTicks(),
            value -> config = config.withDevice(config.device().withButtonHoldTicks(value))));
  }

  HardwiredConfig config() {
    return config;
  }
}
