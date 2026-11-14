package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.BurnRates;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.HumSettings;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.LineSettings;
import com.grahambartley.hardwired.config.PhoneSettings;
import com.grahambartley.hardwired.config.PowerLoads;
import com.grahambartley.hardwired.config.PowerRatings;
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
            KEY + "cellchargeperloadminute",
            KEY + "cellchargeperloadminute.tooltip",
            BurnRates.CHARGE_PER_LOAD_MINUTE_MIN,
            BurnRates.CHARGE_PER_LOAD_MINUTE_MAX,
            config.power().burn().cellChargePerLoadMinute(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withBurn(config.power().burn().withCellChargePerLoadMinute(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "generatorchargeperloadminute",
            KEY + "generatorchargeperloadminute.tooltip",
            BurnRates.CHARGE_PER_LOAD_MINUTE_MIN,
            BurnRates.CHARGE_PER_LOAD_MINUTE_MAX,
            config.power().burn().generatorChargePerLoadMinute(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withBurn(
                                config.power().burn().withGeneratorChargePerLoadMinute(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "sparechargeperloadminute",
            KEY + "sparechargeperloadminute.tooltip",
            BurnRates.CHARGE_PER_LOAD_MINUTE_MIN,
            BurnRates.CHARGE_PER_LOAD_MINUTE_MAX,
            config.power().burn().spareChargePerLoadMinute(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withBurn(config.power().burn().withSpareChargePerLoadMinute(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "cellrating",
            KEY + "cellrating.tooltip",
            PowerRatings.CELL_RATING_MIN,
            PowerRatings.CELL_RATING_MAX,
            config.power().ratings().cellRating(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withRatings(config.power().ratings().withCellRating(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "generatorrating",
            KEY + "generatorrating.tooltip",
            PowerRatings.GENERATOR_RATING_MIN,
            PowerRatings.GENERATOR_RATING_MAX,
            config.power().ratings().generatorRating(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withRatings(config.power().ratings().withGeneratorRating(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "solarrating",
            KEY + "solarrating.tooltip",
            PowerRatings.SOLAR_RATING_MIN,
            PowerRatings.SOLAR_RATING_MAX,
            config.power().ratings().solarRating(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withRatings(config.power().ratings().withSolarRating(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "dynamorating",
            KEY + "dynamorating.tooltip",
            PowerRatings.DYNAMO_RATING_MIN,
            PowerRatings.DYNAMO_RATING_MAX,
            config.power().ratings().dynamoRating(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withRatings(config.power().ratings().withDynamoRating(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "lampload",
            KEY + "lampload.tooltip",
            PowerLoads.LAMP_LOAD_MIN,
            PowerLoads.LAMP_LOAD_MAX,
            config.power().loads().lampLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withLampLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "streetlightload",
            KEY + "streetlightload.tooltip",
            PowerLoads.STREETLIGHT_LOAD_MIN,
            PowerLoads.STREETLIGHT_LOAD_MAX,
            config.power().loads().streetlightLoad(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withLoads(config.power().loads().withStreetlightLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "logicload",
            KEY + "logicload.tooltip",
            PowerLoads.LOGIC_LOAD_MIN,
            PowerLoads.LOGIC_LOAD_MAX,
            config.power().loads().logicLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withLogicLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "vanillaload",
            KEY + "vanillaload.tooltip",
            PowerLoads.VANILLA_LOAD_MIN,
            PowerLoads.VANILLA_LOAD_MAX,
            config.power().loads().vanillaLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withVanillaLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "radioload",
            KEY + "radioload.tooltip",
            PowerLoads.RADIO_LOAD_MIN,
            PowerLoads.RADIO_LOAD_MAX,
            config.power().loads().radioLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withRadioLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "speakerload",
            KEY + "speakerload.tooltip",
            PowerLoads.SPEAKER_LOAD_MIN,
            PowerLoads.SPEAKER_LOAD_MAX,
            config.power().loads().speakerLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withSpeakerLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "intercomload",
            KEY + "intercomload.tooltip",
            PowerLoads.INTERCOM_LOAD_MIN,
            PowerLoads.INTERCOM_LOAD_MAX,
            config.power().loads().intercomLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withIntercomLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "alarmload",
            KEY + "alarmload.tooltip",
            PowerLoads.ALARM_LOAD_MIN,
            PowerLoads.ALARM_LOAD_MAX,
            config.power().loads().alarmLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withAlarmLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "broadcastload",
            KEY + "broadcastload.tooltip",
            PowerLoads.BROADCAST_LOAD_MIN,
            PowerLoads.BROADCAST_LOAD_MAX,
            config.power().loads().broadcastLoad(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withLoads(config.power().loads().withBroadcastLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "liftload",
            KEY + "liftload.tooltip",
            PowerLoads.LIFT_LOAD_MIN,
            PowerLoads.LIFT_LOAD_MAX,
            config.power().loads().liftLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withLiftLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "telephoneload",
            KEY + "telephoneload.tooltip",
            PowerLoads.TELEPHONE_LOAD_MIN,
            PowerLoads.TELEPHONE_LOAD_MAX,
            config.power().loads().telephoneLoad(),
            value ->
                config =
                    config.withPower(
                        config
                            .power()
                            .withLoads(config.power().loads().withTelephoneLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "exchangeload",
            KEY + "exchangeload.tooltip",
            PowerLoads.EXCHANGE_LOAD_MIN,
            PowerLoads.EXCHANGE_LOAD_MAX,
            config.power().loads().exchangeLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withLoads(config.power().loads().withExchangeLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "humrange",
            KEY + "humrange.tooltip",
            HumSettings.HUM_RANGE_MIN,
            HumSettings.HUM_RANGE_MAX,
            config.power().hum().humRange(),
            value ->
                config =
                    config.withPower(
                        config.power().withHum(config.power().hum().withHumRange(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "humstartload",
            KEY + "humstartload.tooltip",
            HumSettings.HUM_START_LOAD_MIN,
            HumSettings.HUM_START_LOAD_MAX,
            config.power().hum().humStartLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withHum(config.power().hum().withHumStartLoad(value)))));
    list.addRow(
        rows.intSlider(
            KEY + "humfullload",
            KEY + "humfullload.tooltip",
            HumSettings.HUM_FULL_LOAD_MIN,
            HumSettings.HUM_FULL_LOAD_MAX,
            config.power().hum().humFullLoad(),
            value ->
                config =
                    config.withPower(
                        config.power().withHum(config.power().hum().withHumFullLoad(value)))));
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
    list.addRow(
        rows.intSlider(
            KEY + "phoneringseconds",
            KEY + "phoneringseconds.tooltip",
            PhoneSettings.RING_SECONDS_MIN,
            PhoneSettings.RING_SECONDS_MAX,
            config.phone().phoneRingSeconds(),
            value -> config = config.withPhone(config.phone().withPhoneRingSeconds(value))));
    list.addRow(
        rows.intSlider(
            KEY + "exchangelines",
            KEY + "exchangelines.tooltip",
            PhoneSettings.EXCHANGE_LINES_MIN,
            PhoneSettings.EXCHANGE_LINES_MAX,
            config.phone().exchangeLines(),
            value -> config = config.withPhone(config.phone().withExchangeLines(value))));
    list.addRow(
        rows.toggle(
            KEY + "voicecalls",
            config.phone().voiceCalls(),
            value -> config = config.withPhone(config.phone().withVoiceCalls(value))));
    list.addRow(
        rows.intSlider(
            KEY + "polespanblocks",
            KEY + "polespanblocks.tooltip",
            LineSettings.SPAN_BLOCKS_MIN,
            LineSettings.SPAN_BLOCKS_MAX,
            config.line().poleSpanBlocks(),
            value -> config = config.withLine(config.line().withPoleSpanBlocks(value))));
    list.addRow(
        rows.intSlider(
            KEY + "spanblocksperwire",
            KEY + "spanblocksperwire.tooltip",
            LineSettings.BLOCKS_PER_WIRE_MIN,
            LineSettings.BLOCKS_PER_WIRE_MAX,
            config.line().spanBlocksPerWire(),
            value -> config = config.withLine(config.line().withSpanBlocksPerWire(value))));
  }

  HardwiredConfig config() {
    return config;
  }
}
