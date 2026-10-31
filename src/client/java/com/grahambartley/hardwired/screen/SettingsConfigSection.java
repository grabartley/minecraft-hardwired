package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.PowerSettings;
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
    list.addRow(rows.header(KEY + "power"));
    list.addRow(
        rows.intSlider(
            KEY + "cellcapacity",
            KEY + "cellcapacity.tooltip",
            PowerSettings.CELL_CAPACITY_MIN,
            PowerSettings.CELL_CAPACITY_MAX,
            config.power().cellCapacity(),
            value -> config = config.withPower(config.power().withCellCapacity(value))));
    list.addRow(
        rows.intSlider(
            KEY + "cellchargeperdust",
            KEY + "cellchargeperdust.tooltip",
            PowerSettings.DUST_CHARGE_MIN,
            PowerSettings.DUST_CHARGE_MAX,
            config.power().dustCharge(),
            value -> config = config.withPower(config.power().withDustCharge(value))));
    list.addRow(
        rows.intSlider(
            KEY + "solarchargeperminute",
            KEY + "solarchargeperminute.tooltip",
            PowerSettings.SOLAR_CHARGE_MIN,
            PowerSettings.SOLAR_CHARGE_MAX,
            config.power().solarChargePerMinute(),
            value -> config = config.withPower(config.power().withSolarChargePerMinute(value))));
    list.addRow(
        rows.intSlider(
            KEY + "lightningcharge",
            KEY + "lightningcharge.tooltip",
            PowerSettings.LIGHTNING_CHARGE_MIN,
            PowerSettings.LIGHTNING_CHARGE_MAX,
            config.power().lightningCharge(),
            value -> config = config.withPower(config.power().withLightningCharge(value))));
    list.addRow(
        rows.intSlider(
            KEY + "alarmchargepersecond",
            KEY + "alarmchargepersecond.tooltip",
            PowerSettings.ALARM_CHARGE_MIN,
            PowerSettings.ALARM_CHARGE_MAX,
            config.power().alarmChargePerSecond(),
            value -> config = config.withPower(config.power().withAlarmChargePerSecond(value))));
    list.addRow(
        rows.intSlider(
            KEY + "radiochargepersecond",
            KEY + "radiochargepersecond.tooltip",
            PowerSettings.RADIO_CHARGE_MIN,
            PowerSettings.RADIO_CHARGE_MAX,
            config.power().radioChargePerSecond(),
            value -> config = config.withPower(config.power().withRadioChargePerSecond(value))));
    list.addRow(
        rows.intSlider(
            KEY + "speakerchargepersecond",
            KEY + "speakerchargepersecond.tooltip",
            PowerSettings.SPEAKER_CHARGE_MIN,
            PowerSettings.SPEAKER_CHARGE_MAX,
            config.power().speakerChargePerSecond(),
            value -> config = config.withPower(config.power().withSpeakerChargePerSecond(value))));
    list.addRow(
        rows.intSlider(
            KEY + "intercomchargeperline",
            KEY + "intercomchargeperline.tooltip",
            PowerSettings.INTERCOM_CHARGE_MIN,
            PowerSettings.INTERCOM_CHARGE_MAX,
            config.power().intercomChargePerLine(),
            value -> config = config.withPower(config.power().withIntercomChargePerLine(value))));
    list.addRow(rows.header(KEY + "lift"));
    list.addRow(
        rows.intSlider(
            KEY + "liftticksperblock",
            KEY + "liftticksperblock.tooltip",
            LiftSettings.TICKS_PER_BLOCK_MIN,
            LiftSettings.TICKS_PER_BLOCK_MAX,
            config.lift().ticksPerBlock(),
            value -> config = config.withLift(config.lift().withTicksPerBlock(value))));
    list.addRow(
        rows.intSlider(
            KEY + "liftchargeperblock",
            KEY + "liftchargeperblock.tooltip",
            LiftSettings.CHARGE_PER_BLOCK_MIN,
            LiftSettings.CHARGE_PER_BLOCK_MAX,
            config.lift().chargePerBlock(),
            value -> config = config.withLift(config.lift().withChargePerBlock(value))));
    list.addRow(rows.header(KEY + "circuit"));
    list.addRow(
        rows.intSlider(
            KEY + "circuitmaxparts",
            KEY + "circuitmaxparts.tooltip",
            CircuitSettings.MAX_PARTS_MIN,
            CircuitSettings.MAX_PARTS_MAX,
            config.circuit().maxParts(),
            value -> config = config.withCircuit(config.circuit().withMaxParts(value))));
    list.addRow(
        rows.intSlider(
            KEY + "circuitpartsperredstone",
            KEY + "circuitpartsperredstone.tooltip",
            CircuitSettings.PARTS_PER_REDSTONE_MIN,
            CircuitSettings.PARTS_PER_REDSTONE_MAX,
            config.circuit().partsPerRedstone(),
            value -> config = config.withCircuit(config.circuit().withPartsPerRedstone(value))));
  }

  HardwiredConfig config() {
    return config;
  }
}
