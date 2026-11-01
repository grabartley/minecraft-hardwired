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
            KEY + "generatorcapacity",
            KEY + "generatorcapacity.tooltip",
            PowerSettings.GENERATOR_CAPACITY_MIN,
            PowerSettings.GENERATOR_CAPACITY_MAX,
            config.power().generatorCapacity(),
            value -> config = config.withPower(config.power().withGeneratorCapacity(value))));
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
            KEY + "lightningcharge",
            KEY + "lightningcharge.tooltip",
            PowerSettings.LIGHTNING_CHARGE_MIN,
            PowerSettings.LIGHTNING_CHARGE_MAX,
            config.power().lightningCharge(),
            value -> config = config.withPower(config.power().withLightningCharge(value))));
    list.addRow(
        rows.intSlider(
            KEY + "chargeperloadminute",
            KEY + "chargeperloadminute.tooltip",
            PowerSettings.CHARGE_PER_LOAD_MINUTE_MIN,
            PowerSettings.CHARGE_PER_LOAD_MINUTE_MAX,
            config.power().chargePerLoadMinute(),
            value -> config = config.withPower(config.power().withChargePerLoadMinute(value))));
    list.addRow(
        rows.intSlider(
            KEY + "cellrating",
            KEY + "cellrating.tooltip",
            PowerSettings.CELL_RATING_MIN,
            PowerSettings.CELL_RATING_MAX,
            config.power().cellRating(),
            value -> config = config.withPower(config.power().withCellRating(value))));
    list.addRow(
        rows.intSlider(
            KEY + "generatorrating",
            KEY + "generatorrating.tooltip",
            PowerSettings.GENERATOR_RATING_MIN,
            PowerSettings.GENERATOR_RATING_MAX,
            config.power().generatorRating(),
            value -> config = config.withPower(config.power().withGeneratorRating(value))));
    list.addRow(
        rows.intSlider(
            KEY + "solarrating",
            KEY + "solarrating.tooltip",
            PowerSettings.SOLAR_RATING_MIN,
            PowerSettings.SOLAR_RATING_MAX,
            config.power().solarRating(),
            value -> config = config.withPower(config.power().withSolarRating(value))));
    list.addRow(
        rows.intSlider(
            KEY + "dynamorating",
            KEY + "dynamorating.tooltip",
            PowerSettings.DYNAMO_RATING_MIN,
            PowerSettings.DYNAMO_RATING_MAX,
            config.power().dynamoRating(),
            value -> config = config.withPower(config.power().withDynamoRating(value))));
    list.addRow(
        rows.intSlider(
            KEY + "lampload",
            KEY + "lampload.tooltip",
            PowerSettings.LAMP_LOAD_MIN,
            PowerSettings.LAMP_LOAD_MAX,
            config.power().lampLoad(),
            value -> config = config.withPower(config.power().withLampLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "streetlightload",
            KEY + "streetlightload.tooltip",
            PowerSettings.STREETLIGHT_LOAD_MIN,
            PowerSettings.STREETLIGHT_LOAD_MAX,
            config.power().streetlightLoad(),
            value -> config = config.withPower(config.power().withStreetlightLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "logicload",
            KEY + "logicload.tooltip",
            PowerSettings.LOGIC_LOAD_MIN,
            PowerSettings.LOGIC_LOAD_MAX,
            config.power().logicLoad(),
            value -> config = config.withPower(config.power().withLogicLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "vanillaload",
            KEY + "vanillaload.tooltip",
            PowerSettings.VANILLA_LOAD_MIN,
            PowerSettings.VANILLA_LOAD_MAX,
            config.power().vanillaLoad(),
            value -> config = config.withPower(config.power().withVanillaLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "radioload",
            KEY + "radioload.tooltip",
            PowerSettings.RADIO_LOAD_MIN,
            PowerSettings.RADIO_LOAD_MAX,
            config.power().radioLoad(),
            value -> config = config.withPower(config.power().withRadioLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "speakerload",
            KEY + "speakerload.tooltip",
            PowerSettings.SPEAKER_LOAD_MIN,
            PowerSettings.SPEAKER_LOAD_MAX,
            config.power().speakerLoad(),
            value -> config = config.withPower(config.power().withSpeakerLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "intercomload",
            KEY + "intercomload.tooltip",
            PowerSettings.INTERCOM_LOAD_MIN,
            PowerSettings.INTERCOM_LOAD_MAX,
            config.power().intercomLoad(),
            value -> config = config.withPower(config.power().withIntercomLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "alarmload",
            KEY + "alarmload.tooltip",
            PowerSettings.ALARM_LOAD_MIN,
            PowerSettings.ALARM_LOAD_MAX,
            config.power().alarmLoad(),
            value -> config = config.withPower(config.power().withAlarmLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "broadcastload",
            KEY + "broadcastload.tooltip",
            PowerSettings.BROADCAST_LOAD_MIN,
            PowerSettings.BROADCAST_LOAD_MAX,
            config.power().broadcastLoad(),
            value -> config = config.withPower(config.power().withBroadcastLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "liftload",
            KEY + "liftload.tooltip",
            PowerSettings.LIFT_LOAD_MIN,
            PowerSettings.LIFT_LOAD_MAX,
            config.power().liftLoad(),
            value -> config = config.withPower(config.power().withLiftLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "humrange",
            KEY + "humrange.tooltip",
            PowerSettings.HUM_RANGE_MIN,
            PowerSettings.HUM_RANGE_MAX,
            config.power().humRange(),
            value -> config = config.withPower(config.power().withHumRange(value))));
    list.addRow(
        rows.intSlider(
            KEY + "humstartload",
            KEY + "humstartload.tooltip",
            PowerSettings.HUM_START_LOAD_MIN,
            PowerSettings.HUM_START_LOAD_MAX,
            config.power().humStartLoad(),
            value -> config = config.withPower(config.power().withHumStartLoad(value))));
    list.addRow(
        rows.intSlider(
            KEY + "humfullload",
            KEY + "humfullload.tooltip",
            PowerSettings.HUM_FULL_LOAD_MIN,
            PowerSettings.HUM_FULL_LOAD_MAX,
            config.power().humFullLoad(),
            value -> config = config.withPower(config.power().withHumFullLoad(value))));
    list.addRow(rows.header(KEY + "lift"));
    list.addRow(
        rows.intSlider(
            KEY + "liftticksperblock",
            KEY + "liftticksperblock.tooltip",
            LiftSettings.TICKS_PER_BLOCK_MIN,
            LiftSettings.TICKS_PER_BLOCK_MAX,
            config.lift().ticksPerBlock(),
            value -> config = config.withLift(config.lift().withTicksPerBlock(value))));
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
    list.addRow(
        rows.toggle(
            KEY + "voicebroadcasts",
            config.radio().voiceBroadcasts(),
            value -> config = config.withRadio(config.radio().withVoiceBroadcasts(value))));
  }

  HardwiredConfig config() {
    return config;
  }
}
