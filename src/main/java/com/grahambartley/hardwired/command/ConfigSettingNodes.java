package com.grahambartley.hardwired.command;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.ConfigRefusals;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.server.ServerConfigService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

final class ConfigSettingNodes {

  private static final String ENABLED = "enabled";

  private ConfigSettingNodes() {}

  static LiteralArgumentBuilder<ServerCommandSource> intSetting(
      final String name,
      final String argument,
      final int min,
      final int max,
      final BiFunction<HardwiredConfig, Integer, HardwiredConfig> update,
      final IntFunction<String> display) {
    return CommandManager.literal(name)
        .then(intArgument(name, argument, min, max, update, display));
  }

  static LiteralArgumentBuilder<ServerCommandSource> boolSetting(
      final String name, final BiFunction<HardwiredConfig, Boolean, HardwiredConfig> update) {
    return CommandManager.literal(name)
        .then(
            CommandManager.argument(ENABLED, BoolArgumentType.bool())
                .executes(
                    ctx -> {
                      final boolean value = BoolArgumentType.getBool(ctx, ENABLED);
                      return applyUpdate(
                          ctx,
                          update.apply(Hardwired.SERVER_CONFIG, value),
                          name,
                          Boolean.toString(value));
                    }));
  }

  static RequiredArgumentBuilder<ServerCommandSource, Integer> intArgument(
      final String label,
      final String argument,
      final int min,
      final int max,
      final BiFunction<HardwiredConfig, Integer, HardwiredConfig> update,
      final IntFunction<String> display) {
    return CommandManager.argument(argument, IntegerArgumentType.integer(min, max))
        .executes(
            ctx -> {
              final int value = IntegerArgumentType.getInteger(ctx, argument);
              return applyUpdate(
                  ctx, update.apply(Hardwired.SERVER_CONFIG, value), label, display.apply(value));
            });
  }

  static int applyUpdate(
      final CommandContext<ServerCommandSource> ctx,
      final HardwiredConfig updated,
      final String label,
      final String displayValue) {
    final ServerCommandSource source = ctx.getSource();
    final Optional<Text> refusal = ConfigRefusals.of(updated);
    if (refusal.isPresent()) {
      source.sendError(refusal.get());
      return 0;
    }
    final boolean ok = ServerConfigService.update(source.getServer(), updated);
    if (!ok) {
      source.sendError(Text.translatable("command.hardwired.update.failed"));
      return 0;
    }
    source.sendFeedback(
        () -> Text.translatable("command.hardwired.update.success", label, displayValue), true);
    return 1;
  }
}
