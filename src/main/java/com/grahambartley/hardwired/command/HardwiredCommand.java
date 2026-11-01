package com.grahambartley.hardwired.command;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.HumSettings;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.config.PowerLoads;
import com.grahambartley.hardwired.config.PowerRatings;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.config.WireSettings;
import com.grahambartley.hardwired.server.ServerConfigService;
import com.grahambartley.hardwired.wire.WireOverrides;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class HardwiredCommand {

  private record IntOption(
      String name,
      String argument,
      int min,
      int max,
      Function<HardwiredConfig, Integer> read,
      BiFunction<HardwiredConfig, Integer, HardwiredConfig> write) {}

  private record BoolOption(
      String name,
      Function<HardwiredConfig, Boolean> read,
      BiFunction<HardwiredConfig, Boolean, HardwiredConfig> write) {}

  private static final List<BoolOption> SWITCHES =
      List.of(
          new BoolOption(
              "voicebroadcasts",
              config -> config.radio().voiceBroadcasts(),
              (config, value) -> config.withRadio(config.radio().withVoiceBroadcasts(value))));

  private static final List<IntOption> OPTIONS =
      List.of(
          new IntOption(
              "wireviewrange",
              "blocks",
              WireSettings.VIEW_RANGE_MIN,
              WireSettings.VIEW_RANGE_MAX,
              config -> config.wire().viewRangeBlocks(),
              (config, value) -> config.withWire(config.wire().withViewRangeBlocks(value))),
          new IntOption(
              "maxnetworksize",
              "faces",
              WireSettings.NETWORK_SIZE_MIN,
              WireSettings.NETWORK_SIZE_MAX,
              config -> config.wire().maxNetworkSize(),
              (config, value) -> config.withWire(config.wire().withMaxNetworkSize(value))),
          new IntOption(
              "lampagerate",
              "percent",
              LampSettings.AGE_RATE_MIN,
              LampSettings.AGE_RATE_MAX,
              config -> config.lamp().ageRateMultiplierPercent(),
              (config, value) ->
                  config.withLamp(config.lamp().withAgeRateMultiplierPercent(value))),
          new IntOption(
              "motionsensorrange",
              "blocks",
              DeviceSettings.MOTION_RANGE_MIN,
              DeviceSettings.MOTION_RANGE_MAX,
              config -> config.device().motionSensorRangeBlocks(),
              (config, value) ->
                  config.withDevice(config.device().withMotionSensorRangeBlocks(value))),
          new IntOption(
              "buttonholdticks",
              "ticks",
              DeviceSettings.BUTTON_HOLD_MIN,
              DeviceSettings.BUTTON_HOLD_MAX,
              config -> config.device().buttonHoldTicks(),
              (config, value) -> config.withDevice(config.device().withButtonHoldTicks(value))),
          new IntOption(
              "intercomrange",
              "blocks",
              DeviceSettings.INTERCOM_RANGE_MIN,
              DeviceSettings.INTERCOM_RANGE_MAX,
              config -> config.device().intercomRangeBlocks(),
              (config, value) -> config.withDevice(config.device().withIntercomRangeBlocks(value))),
          new IntOption(
              "cellcapacity",
              "charge",
              PowerSettings.CELL_CAPACITY_MIN,
              PowerSettings.CELL_CAPACITY_MAX,
              config -> config.power().cellCapacity(),
              (config, value) -> config.withPower(config.power().withCellCapacity(value))),
          new IntOption(
              "generatorcapacity",
              "charge",
              PowerSettings.GENERATOR_CAPACITY_MIN,
              PowerSettings.GENERATOR_CAPACITY_MAX,
              config -> config.power().generatorCapacity(),
              (config, value) -> config.withPower(config.power().withGeneratorCapacity(value))),
          new IntOption(
              "cellchargeperdust",
              "charge",
              PowerSettings.DUST_CHARGE_MIN,
              PowerSettings.DUST_CHARGE_MAX,
              config -> config.power().dustCharge(),
              (config, value) -> config.withPower(config.power().withDustCharge(value))),
          new IntOption(
              "lightningcharge",
              "charge",
              PowerSettings.LIGHTNING_CHARGE_MIN,
              PowerSettings.LIGHTNING_CHARGE_MAX,
              config -> config.power().lightningCharge(),
              (config, value) -> config.withPower(config.power().withLightningCharge(value))),
          new IntOption(
              "chargeperloadminute",
              "charge",
              PowerSettings.CHARGE_PER_LOAD_MINUTE_MIN,
              PowerSettings.CHARGE_PER_LOAD_MINUTE_MAX,
              config -> config.power().chargePerLoadMinute(),
              (config, value) -> config.withPower(config.power().withChargePerLoadMinute(value))),
          new IntOption(
              "cellrating",
              "value",
              PowerRatings.CELL_RATING_MIN,
              PowerRatings.CELL_RATING_MAX,
              config -> config.power().ratings().cellRating(),
              (config, value) ->
                  config.withPower(
                      config.power().withRatings(config.power().ratings().withCellRating(value)))),
          new IntOption(
              "generatorrating",
              "value",
              PowerRatings.GENERATOR_RATING_MIN,
              PowerRatings.GENERATOR_RATING_MAX,
              config -> config.power().ratings().generatorRating(),
              (config, value) ->
                  config.withPower(
                      config
                          .power()
                          .withRatings(config.power().ratings().withGeneratorRating(value)))),
          new IntOption(
              "solarrating",
              "value",
              PowerRatings.SOLAR_RATING_MIN,
              PowerRatings.SOLAR_RATING_MAX,
              config -> config.power().ratings().solarRating(),
              (config, value) ->
                  config.withPower(
                      config.power().withRatings(config.power().ratings().withSolarRating(value)))),
          new IntOption(
              "dynamorating",
              "value",
              PowerRatings.DYNAMO_RATING_MIN,
              PowerRatings.DYNAMO_RATING_MAX,
              config -> config.power().ratings().dynamoRating(),
              (config, value) ->
                  config.withPower(
                      config
                          .power()
                          .withRatings(config.power().ratings().withDynamoRating(value)))),
          new IntOption(
              "lampload",
              "value",
              PowerLoads.LAMP_LOAD_MIN,
              PowerLoads.LAMP_LOAD_MAX,
              config -> config.power().loads().lampLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withLampLoad(value)))),
          new IntOption(
              "streetlightload",
              "value",
              PowerLoads.STREETLIGHT_LOAD_MIN,
              PowerLoads.STREETLIGHT_LOAD_MAX,
              config -> config.power().loads().streetlightLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withStreetlightLoad(value)))),
          new IntOption(
              "logicload",
              "value",
              PowerLoads.LOGIC_LOAD_MIN,
              PowerLoads.LOGIC_LOAD_MAX,
              config -> config.power().loads().logicLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withLogicLoad(value)))),
          new IntOption(
              "vanillaload",
              "value",
              PowerLoads.VANILLA_LOAD_MIN,
              PowerLoads.VANILLA_LOAD_MAX,
              config -> config.power().loads().vanillaLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withVanillaLoad(value)))),
          new IntOption(
              "radioload",
              "value",
              PowerLoads.RADIO_LOAD_MIN,
              PowerLoads.RADIO_LOAD_MAX,
              config -> config.power().loads().radioLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withRadioLoad(value)))),
          new IntOption(
              "speakerload",
              "value",
              PowerLoads.SPEAKER_LOAD_MIN,
              PowerLoads.SPEAKER_LOAD_MAX,
              config -> config.power().loads().speakerLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withSpeakerLoad(value)))),
          new IntOption(
              "intercomload",
              "value",
              PowerLoads.INTERCOM_LOAD_MIN,
              PowerLoads.INTERCOM_LOAD_MAX,
              config -> config.power().loads().intercomLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withIntercomLoad(value)))),
          new IntOption(
              "alarmload",
              "value",
              PowerLoads.ALARM_LOAD_MIN,
              PowerLoads.ALARM_LOAD_MAX,
              config -> config.power().loads().alarmLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withAlarmLoad(value)))),
          new IntOption(
              "broadcastload",
              "value",
              PowerLoads.BROADCAST_LOAD_MIN,
              PowerLoads.BROADCAST_LOAD_MAX,
              config -> config.power().loads().broadcastLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withBroadcastLoad(value)))),
          new IntOption(
              "liftload",
              "value",
              PowerLoads.LIFT_LOAD_MIN,
              PowerLoads.LIFT_LOAD_MAX,
              config -> config.power().loads().liftLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withLoads(config.power().loads().withLiftLoad(value)))),
          new IntOption(
              "humrange",
              "value",
              HumSettings.HUM_RANGE_MIN,
              HumSettings.HUM_RANGE_MAX,
              config -> config.power().hum().humRange(),
              (config, value) ->
                  config.withPower(
                      config.power().withHum(config.power().hum().withHumRange(value)))),
          new IntOption(
              "humstartload",
              "value",
              HumSettings.HUM_START_LOAD_MIN,
              HumSettings.HUM_START_LOAD_MAX,
              config -> config.power().hum().humStartLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withHum(config.power().hum().withHumStartLoad(value)))),
          new IntOption(
              "humfullload",
              "value",
              HumSettings.HUM_FULL_LOAD_MIN,
              HumSettings.HUM_FULL_LOAD_MAX,
              config -> config.power().hum().humFullLoad(),
              (config, value) ->
                  config.withPower(
                      config.power().withHum(config.power().hum().withHumFullLoad(value)))),
          new IntOption(
              "liftticksperblock",
              "ticks",
              LiftSettings.TICKS_PER_BLOCK_MIN,
              LiftSettings.TICKS_PER_BLOCK_MAX,
              config -> config.lift().ticksPerBlock(),
              (config, value) -> config.withLift(config.lift().withTicksPerBlock(value))),
          new IntOption(
              "circuitmaxparts",
              "parts",
              CircuitSettings.MAX_PARTS_MIN,
              CircuitSettings.MAX_PARTS_MAX,
              config -> config.circuit().maxParts(),
              (config, value) -> config.withCircuit(config.circuit().withMaxParts(value))),
          new IntOption(
              "circuitpartsperredstone",
              "parts",
              CircuitSettings.PARTS_PER_REDSTONE_MIN,
              CircuitSettings.PARTS_PER_REDSTONE_MAX,
              config -> config.circuit().partsPerRedstone(),
              (config, value) -> config.withCircuit(config.circuit().withPartsPerRedstone(value))),
          new IntOption(
              "towerminmasts",
              "masts",
              RadioSettings.TOWER_MIN_MASTS_MIN,
              RadioSettings.TOWER_MIN_MASTS_MAX,
              config -> config.radio().towerMinMasts(),
              (config, value) -> config.withRadio(config.radio().withTowerMinMasts(value))),
          new IntOption(
              "towermaxmasts",
              "masts",
              RadioSettings.TOWER_MAX_MASTS_MIN,
              RadioSettings.TOWER_MAX_MASTS_MAX,
              config -> config.radio().towerMaxMasts(),
              (config, value) -> config.withRadio(config.radio().withTowerMaxMasts(value))),
          new IntOption(
              "towerbasereach",
              "blocks",
              RadioSettings.TOWER_BASE_REACH_MIN,
              RadioSettings.TOWER_BASE_REACH_MAX,
              config -> config.radio().towerBaseReach(),
              (config, value) -> config.withRadio(config.radio().withTowerBaseReach(value))),
          new IntOption(
              "towerreachpermast",
              "blocks",
              RadioSettings.TOWER_REACH_PER_MAST_MIN,
              RadioSettings.TOWER_REACH_PER_MAST_MAX,
              config -> config.radio().towerReachPerMast(),
              (config, value) -> config.withRadio(config.radio().withTowerReachPerMast(value))),
          new IntOption(
              "radiohearing",
              "blocks",
              RadioSettings.RADIO_HEARING_MIN,
              RadioSettings.RADIO_HEARING_MAX,
              config -> config.radio().radioHearing(),
              (config, value) -> config.withRadio(config.radio().withRadioHearing(value))),
          new IntOption(
              "speakerhearing",
              "blocks",
              RadioSettings.SPEAKER_HEARING_MIN,
              RadioSettings.SPEAKER_HEARING_MAX,
              config -> config.radio().speakerHearing(),
              (config, value) -> config.withRadio(config.radio().withSpeakerHearing(value))),
          new IntOption(
              "undergroundfreedepth",
              "blocks",
              RadioSettings.UNDERGROUND_FREE_DEPTH_MIN,
              RadioSettings.UNDERGROUND_FREE_DEPTH_MAX,
              config -> config.radio().undergroundFreeDepth(),
              (config, value) -> config.withRadio(config.radio().withUndergroundFreeDepth(value))),
          new IntOption(
              "undergroundlossdepth",
              "blocks",
              RadioSettings.UNDERGROUND_LOSS_DEPTH_MIN,
              RadioSettings.UNDERGROUND_LOSS_DEPTH_MAX,
              config -> config.radio().undergroundLossDepth(),
              (config, value) -> config.withRadio(config.radio().withUndergroundLossDepth(value))));

  private HardwiredCommand() {}

  public static void register(final CommandDispatcher<ServerCommandSource> dispatcher) {
    final LiteralArgumentBuilder<ServerCommandSource> config =
        CommandManager.literal("config").requires(HardwiredCommand::isOp);
    for (final IntOption option : OPTIONS) {
      config.then(
          ConfigSettingNodes.intSetting(
              option.name(),
              option.argument(),
              option.min(),
              option.max(),
              option.write(),
              Integer::toString));
    }
    for (final BoolOption option : SWITCHES) {
      config.then(ConfigSettingNodes.boolSetting(option.name(), option.write()));
    }
    dispatcher.register(
        CommandManager.literal("hardwired")
            .executes(HardwiredCommand::help)
            .then(
                CommandManager.literal("status")
                    .requires(HardwiredCommand::isOp)
                    .executes(HardwiredCommand::status))
            .then(
                CommandManager.literal("override")
                    .requires(HardwiredCommand::isOp)
                    .executes(HardwiredCommand::override))
            .then(config.then(CommandManager.literal("reset").executes(HardwiredCommand::reset))));
  }

  private static boolean isOp(final ServerCommandSource source) {
    return source.hasPermissionLevel(ServerConfigService.OP_PERMISSION_LEVEL);
  }

  private static int override(final CommandContext<ServerCommandSource> ctx)
      throws CommandSyntaxException {
    final ServerCommandSource source = ctx.getSource();
    final boolean on =
        WireOverrides.of(source.getServer()).toggle(source.getPlayerOrThrow().getUuid());
    source.sendFeedback(
        () ->
            Text.translatable(
                on ? "command.hardwired.override.on" : "command.hardwired.override.off"),
        true);
    return 1;
  }

  private static int help(final CommandContext<ServerCommandSource> ctx) {
    for (final IntOption option : OPTIONS) {
      ctx.getSource()
          .sendFeedback(
              () ->
                  Text.translatable(
                      "command.hardwired.help.setting", option.name(), option.min(), option.max()),
              false);
    }
    for (final BoolOption option : SWITCHES) {
      ctx.getSource()
          .sendFeedback(
              () -> Text.translatable("command.hardwired.help.switch", option.name()), false);
    }
    if (isOp(ctx.getSource())) {
      ctx.getSource()
          .sendFeedback(() -> Text.translatable("command.hardwired.help.override"), false);
    }
    return 1;
  }

  private static int status(final CommandContext<ServerCommandSource> ctx) {
    for (final IntOption option : OPTIONS) {
      ctx.getSource()
          .sendFeedback(
              () ->
                  Text.translatable(
                      "command.hardwired.status.setting",
                      option.name(),
                      option.read().apply(Hardwired.SERVER_CONFIG)),
              false);
    }
    for (final BoolOption option : SWITCHES) {
      ctx.getSource()
          .sendFeedback(
              () ->
                  Text.translatable(
                      "command.hardwired.status.setting",
                      option.name(),
                      option.read().apply(Hardwired.SERVER_CONFIG).toString()),
              false);
    }
    return 1;
  }

  private static int reset(final CommandContext<ServerCommandSource> ctx) {
    return ConfigSettingNodes.applyUpdate(ctx, HardwiredConfig.defaults(), "config", "defaults");
  }
}
