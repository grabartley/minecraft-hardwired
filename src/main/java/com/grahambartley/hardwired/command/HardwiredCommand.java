package com.grahambartley.hardwired.command;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.DeviceSettings;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.config.LampSettings;
import com.grahambartley.hardwired.config.WireSettings;
import com.grahambartley.hardwired.server.ServerConfigService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
              (config, value) -> config.withDevice(config.device().withButtonHoldTicks(value))));

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
    dispatcher.register(
        CommandManager.literal("hardwired")
            .executes(HardwiredCommand::help)
            .then(
                CommandManager.literal("status")
                    .requires(HardwiredCommand::isOp)
                    .executes(HardwiredCommand::status))
            .then(config.then(CommandManager.literal("reset").executes(HardwiredCommand::reset))));
  }

  private static boolean isOp(final ServerCommandSource source) {
    return source.hasPermissionLevel(ServerConfigService.OP_PERMISSION_LEVEL);
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
    return 1;
  }

  private static int reset(final CommandContext<ServerCommandSource> ctx) {
    return ConfigSettingNodes.applyUpdate(ctx, HardwiredConfig.defaults(), "config", "defaults");
  }
}
