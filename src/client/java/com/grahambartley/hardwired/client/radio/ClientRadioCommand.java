package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientRadioSettings;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public final class ClientRadioCommand {

  private static final String ROOT = "hardwiredclient";
  private static final String CONFIG = "config";
  private static final String VALUE = "value";
  private static final String DEFAULT_MICROPHONE = "default";

  private ClientRadioCommand() {}

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
                                            .executes(ClientRadioCommand::setNoise)))
                            .then(
                                ClientCommandManager.literal("electrichum")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE,
                                                IntegerArgumentType.integer(
                                                    ClientRadioSettings.NOISE_MIN,
                                                    ClientRadioSettings.NOISE_MAX))
                                            .executes(ClientRadioCommand::setHum)))
                            .then(
                                ClientCommandManager.literal("hearvoicebroadcasts")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE, BoolArgumentType.bool())
                                            .executes(ClientRadioCommand::setHearing)))
                            .then(
                                ClientCommandManager.literal("microphone")
                                    .then(
                                        ClientCommandManager.argument(
                                                VALUE, StringArgumentType.greedyString())
                                            .suggests(
                                                (context, builder) ->
                                                    CommandSource.suggestMatching(
                                                        MicrophoneSource.names(), builder))
                                            .executes(ClientRadioCommand::setMicrophone))))));
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

  private static int apply(
      CommandContext<FabricClientCommandSource> context,
      ClientRadioSettings settings,
      String label,
      String shown) {
    ClientSettingsStore.update(settings);
    context
        .getSource()
        .sendFeedback(Text.translatable("command.hardwired.update.success", label, shown));
    return 1;
  }
}
