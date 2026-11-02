package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.client.radio.MicrophoneSource;
import com.grahambartley.hardwired.config.ClientDisplaySettings;
import com.grahambartley.hardwired.config.ClientRadioSettings;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.function.BiFunction;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public final class ClientConfigCommand {

  private static final String ROOT = "hardwiredclient";
  private static final String CONFIG = "config";
  private static final String VALUE = "value";
  private static final String DEFAULT_MICROPHONE = "default";

  private ClientConfigCommand() {}

  public static void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registries) ->
            dispatcher.register(
                ClientCommandManager.literal(ROOT)
                    .then(
                        ClientCommandManager.literal(CONFIG)
                            .then(
                                ClientCommandManager.literal("radionoise")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE,
                                                IntegerArgumentType.integer(
                                                    ClientRadioSettings.NOISE_MIN,
                                                    ClientRadioSettings.NOISE_MAX))
                                            .executes(ClientConfigCommand::setNoise)))
                            .then(
                                ClientCommandManager.literal("electrichum")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE,
                                                IntegerArgumentType.integer(
                                                    ClientRadioSettings.NOISE_MIN,
                                                    ClientRadioSettings.NOISE_MAX))
                                            .executes(ClientConfigCommand::setHum)))
                            .then(
                                ClientCommandManager.literal("throughblocks")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE,
                                                IntegerArgumentType.integer(
                                                    ClientDisplaySettings.THROUGH_MIN,
                                                    ClientDisplaySettings.THROUGH_MAX))
                                            .executes(ClientConfigCommand::setThroughBlocks)))
                            .then(
                                ClientCommandManager.literal("onsurface")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE,
                                                IntegerArgumentType.integer(
                                                    ClientDisplaySettings.SURFACE_MIN,
                                                    ClientDisplaySettings.SURFACE_MAX))
                                            .executes(ClientConfigCommand::setOnSurface)))
                            .then(toggle("showmarkers", ClientConfigCommand::setShowMarkers))
                            .then(toggle("colouredlight", ClientConfigCommand::setColouredLight))
                            .then(toggle("hearvoicebroadcasts", ClientConfigCommand::setHearing))
                            .then(
                                ClientCommandManager.literal("microphone")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE, StringArgumentType.greedyString())
                                            .suggests(
                                                (context, builder) ->
                                                    CommandSource.suggestMatching(
                                                        MicrophoneSource.names(), builder))
                                            .executes(ClientConfigCommand::setMicrophone))))));
  }

  private static LiteralArgumentBuilder<FabricClientCommandSource> toggle(
      String name, Command<FabricClientCommandSource> command) {
    return ClientCommandManager.literal(name)
        .then(ClientCommandManager.argument(VALUE, BoolArgumentType.bool()).executes(command));
  }

  private static int setNoise(CommandContext<FabricClientCommandSource> context) {
    int value = IntegerArgumentType.getInteger(context, VALUE);
    return apply(
        context,
        ClientSettingsStore.radio().withRadioNoisePercent(value),
        "radionoise",
        Integer.toString(value));
  }

  private static int setHum(CommandContext<FabricClientCommandSource> context) {
    int value = IntegerArgumentType.getInteger(context, VALUE);
    return apply(
        context,
        ClientSettingsStore.radio().withElectricHumPercent(value),
        "electrichum",
        Integer.toString(value));
  }

  private static int setThroughBlocks(CommandContext<FabricClientCommandSource> context) {
    int value = IntegerArgumentType.getInteger(context, VALUE);
    return applyDisplay(
        context,
        ClientSettingsStore.get().withThroughBlocksPercent(value),
        "throughblocks",
        Integer.toString(value));
  }

  private static int setOnSurface(CommandContext<FabricClientCommandSource> context) {
    int value = IntegerArgumentType.getInteger(context, VALUE);
    return applyDisplay(
        context,
        ClientSettingsStore.get().withOnSurfacePercent(value),
        "onsurface",
        Integer.toString(value));
  }

  private static int setShowMarkers(CommandContext<FabricClientCommandSource> context) {
    return applyDisplayToggle(context, "showmarkers", ClientDisplaySettings::withShowMarkers);
  }

  private static int setColouredLight(CommandContext<FabricClientCommandSource> context) {
    return applyDisplayToggle(context, "colouredlight", ClientDisplaySettings::withColouredLight);
  }

  private static int setHearing(CommandContext<FabricClientCommandSource> context) {
    boolean value = BoolArgumentType.getBool(context, VALUE);
    return apply(
        context,
        ClientSettingsStore.radio().withHearVoiceBroadcasts(value),
        "hearvoicebroadcasts",
        Boolean.toString(value));
  }

  private static int setMicrophone(CommandContext<FabricClientCommandSource> context) {
    String value = StringArgumentType.getString(context, VALUE);
    String chosen =
        value.equals(DEFAULT_MICROPHONE) ? ClientRadioSettings.DEFAULT_MICROPHONE : value;
    return apply(context, ClientSettingsStore.radio().withMicrophone(chosen), "microphone", value);
  }

  private static int applyDisplayToggle(
      CommandContext<FabricClientCommandSource> context,
      String label,
      BiFunction<ClientDisplaySettings, Boolean, ClientDisplaySettings> setter) {
    boolean value = BoolArgumentType.getBool(context, VALUE);
    return applyDisplay(
        context, setter.apply(ClientSettingsStore.get(), value), label, Boolean.toString(value));
  }

  private static int applyDisplay(
      CommandContext<FabricClientCommandSource> context,
      ClientDisplaySettings settings,
      String label,
      String shown) {
    ClientSettingsStore.update(settings);
    return confirm(context, label, shown);
  }

  private static int apply(
      CommandContext<FabricClientCommandSource> context,
      ClientRadioSettings settings,
      String label,
      String shown) {
    ClientSettingsStore.update(settings);
    return confirm(context, label, shown);
  }

  private static int confirm(
      CommandContext<FabricClientCommandSource> context, String label, String shown) {
    context
        .getSource()
        .sendFeedback(Text.translatable("command.hardwired.update.success", label, shown));
    return 1;
  }
}
