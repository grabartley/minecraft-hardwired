package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.config.RadioSettings;
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
    list.addRow(
        rows.intSlider(
            KEY + "intercomrange",
            KEY + "intercomrange.tooltip",
            DeviceSettings.INTERCOM_RANGE_MIN,
            DeviceSettings.INTERCOM_RANGE_MAX,
            config.device().intercomRangeBlocks(),
            value -> config = config.withDevice(config.device().withIntercomRangeBlocks(value))));
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
    list.addRow(
        rows.intSlider(
            KEY + "broadcastchargepersecond",
            KEY + "broadcastchargepersecond.tooltip",
            PowerSettings.BROADCAST_CHARGE_MIN,
            PowerSettings.BROADCAST_CHARGE_MAX,
            config.power().broadcastChargePerSecond(),
            value ->
                config = config.withPower(config.power().withBroadcastChargePerSecond(value))));
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
    list.addRow(rows.header(KEY + "radio"));
    list.addRow(
        rows.intSlider(
            KEY + "towerminmasts",
            KEY + "towerminmasts.tooltip",
            RadioSettings.TOWER_MIN_MASTS_MIN,
            RadioSettings.TOWER_MIN_MASTS_MAX,
            config.radio().towerMinMasts(),
            value -> config = config.withRadio(config.radio().withTowerMinMasts(value))));
    list.addRow(
        rows.intSlider(
            KEY + "towermaxmasts",
            KEY + "towermaxmasts.tooltip",
            RadioSettings.TOWER_MAX_MASTS_MIN,
            RadioSettings.TOWER_MAX_MASTS_MAX,
            config.radio().towerMaxMasts(),
            value -> config = config.withRadio(config.radio().withTowerMaxMasts(value))));
    list.addRow(
        rows.intSlider(
            KEY + "towerbasereach",
            KEY + "towerbasereach.tooltip",
            RadioSettings.TOWER_BASE_REACH_MIN,
            RadioSettings.TOWER_BASE_REACH_MAX,
            config.radio().towerBaseReach(),
            value -> config = config.withRadio(config.radio().withTowerBaseReach(value))));
    list.addRow(
        rows.intSlider(
            KEY + "towerreachpermast",
            KEY + "towerreachpermast.tooltip",
            RadioSettings.TOWER_REACH_PER_MAST_MIN,
            RadioSettings.TOWER_REACH_PER_MAST_MAX,
            config.radio().towerReachPerMast(),
            value -> config = config.withRadio(config.radio().withTowerReachPerMast(value))));
    list.addRow(
        rows.intSlider(
            KEY + "radiohearing",
            KEY + "radiohearing.tooltip",
            RadioSettings.RADIO_HEARING_MIN,
            RadioSettings.RADIO_HEARING_MAX,
            config.radio().radioHearing(),
            value -> config = config.withRadio(config.radio().withRadioHearing(value))));
    list.addRow(
        rows.intSlider(
            KEY + "speakerhearing",
            KEY + "speakerhearing.tooltip",
            RadioSettings.SPEAKER_HEARING_MIN,
            RadioSettings.SPEAKER_HEARING_MAX,
            config.radio().speakerHearing(),
            value -> config = config.withRadio(config.radio().withSpeakerHearing(value))));
    list.addRow(
        rows.intSlider(
            KEY + "undergroundfreedepth",
            KEY + "undergroundfreedepth.tooltip",
            RadioSettings.UNDERGROUND_FREE_DEPTH_MIN,
            RadioSettings.UNDERGROUND_FREE_DEPTH_MAX,
            config.radio().undergroundFreeDepth(),
            value -> config = config.withRadio(config.radio().withUndergroundFreeDepth(value))));
    list.addRow(
        rows.intSlider(
            KEY + "undergroundlossdepth",
            KEY + "undergroundlossdepth.tooltip",
            RadioSettings.UNDERGROUND_LOSS_DEPTH_MIN,
            RadioSettings.UNDERGROUND_LOSS_DEPTH_MAX,
            config.radio().undergroundLossDepth(),
            value -> config = config.withRadio(config.radio().withUndergroundLossDepth(value))));
  }

  HardwiredConfig config() {
    return config;
  }
}
