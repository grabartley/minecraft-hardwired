package com.grahambartley.hardwired;

import com.grahambartley.hardwired.block.AbstractLampBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.block.IntercomBlock;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.client.ClientConfigCommand;
import com.grahambartley.hardwired.client.DeviceHud;
import com.grahambartley.hardwired.client.DeviceReadings;
import com.grahambartley.hardwired.client.LiftCarRenderer;
import com.grahambartley.hardwired.client.WireDrag;
import com.grahambartley.hardwired.client.WireHud;
import com.grahambartley.hardwired.client.WireRenderer;
import com.grahambartley.hardwired.client.WireViewStore;
import com.grahambartley.hardwired.client.anim.AnimatedDeviceRenderer;
import com.grahambartley.hardwired.client.anim.DeviceAnimations;
import com.grahambartley.hardwired.client.power.PowerSounds;
import com.grahambartley.hardwired.client.power.PowerTooltip;
import com.grahambartley.hardwired.client.radio.BroadcastSounds;
import com.grahambartley.hardwired.client.radio.TalkKey;
import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.network.LiftViewPayload;
import com.grahambartley.hardwired.network.MainPanelViewPayload;
import com.grahambartley.hardwired.network.OpenDevicePayload;
import com.grahambartley.hardwired.network.ServerConfigPayloads.SyncServerConfigS2CPayload;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import com.grahambartley.hardwired.network.WireViewClearPayload;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.registry.ModEntities;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.screen.BroadcastDeskScreen;
import com.grahambartley.hardwired.screen.CircuitBoardScreen;
import com.grahambartley.hardwired.screen.ControlPanelScreen;
import com.grahambartley.hardwired.screen.ControllerScreen;
import com.grahambartley.hardwired.screen.IntercomScreen;
import com.grahambartley.hardwired.screen.JunctionScreen;
import com.grahambartley.hardwired.screen.LiftScreen;
import com.grahambartley.hardwired.screen.MainPanelScreen;
import com.grahambartley.hardwired.screen.RadioScreen;
import com.grahambartley.hardwired.wire.Hue;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

public class HardwiredClient implements ClientModInitializer {

  private static final int OPAQUE = 0xFF000000;
  private static final int NO_TINT = -1;
  private static final int TINTED_LAYER = 1;
  private static final int LAMP_TINT_FLOOR = 7;
  private static final List<BlockEntityType<? extends BlockEntity>> ANIMATED_TYPES =
      List.of(
          ModBlockEntities.ANIMATED_DEVICE,
          ModBlockEntities.REDSTONE_CELL,
          ModBlockEntities.REDSTONE_GENERATOR,
          ModBlockEntities.SOLAR_PANEL,
          ModBlockEntities.LODESTONE_DYNAMO,
          ModBlockEntities.MASTER_SWITCH,
          ModBlockEntities.CONTROLLER,
          ModBlockEntities.ELECTRONIC,
          ModBlockEntities.RADIO,
          ModBlockEntities.SPEAKER,
          ModBlockEntities.BROADCAST_DESK);

  @Override
  public void onInitializeClient() {
    ClientPlayNetworking.registerGlobalReceiver(
        WireChunkPayload.ID, (payload, context) -> WireViewStore.put(payload));
    ClientPlayNetworking.registerGlobalReceiver(
        WireViewClearPayload.ID, (payload, context) -> WireViewStore.clear());
    ClientPlayNetworking.registerGlobalReceiver(
        WireInspectResultPayload.ID, (payload, context) -> WireDrag.acceptResult(payload));
    ClientPlayNetworking.registerGlobalReceiver(
        OpenDevicePayload.ID, (payload, context) -> openDevice(context.client(), payload));
    ClientPlayNetworking.registerGlobalReceiver(
        MainPanelViewPayload.ID, (payload, context) -> showMainPanel(context.client(), payload));
    ClientPlayNetworking.registerGlobalReceiver(
        LiftViewPayload.ID,
        (payload, context) -> context.client().setScreen(new LiftScreen(payload)));
    ClientPlayNetworking.registerGlobalReceiver(
        CircuitResultPayload.ID,
        (payload, context) -> showCircuitResult(context.client(), payload));
    ClientPlayNetworking.registerGlobalReceiver(
        SyncServerConfigS2CPayload.ID,
        (payload, context) -> SyncedServerConfig.apply(payload.config()));
    SyncedServerConfig.resetOnDisconnect();
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WireViewStore.clear());
    ClientTickEvents.END_CLIENT_TICK.register(WireDrag::tick);
    UseBlockCallback.EVENT.register(WireDrag::onUseBlock);
    WorldRenderEvents.LAST.register(WireRenderer::render);
    HudRenderCallback.EVENT.register(WireHud::render);
    HudRenderCallback.EVENT.register(DeviceHud::render);
    registerColors();
    ClientSettingsStore.load(FabricLoader.getInstance().getConfigDir());
    DeviceAnimations.register();
    ANIMATED_TYPES.forEach(HardwiredClient::registerAnimated);
    LiftCarRenderer.registerModel();
    BroadcastSounds.register();
    PowerSounds.register();
    DeviceReadings.register();
    PowerTooltip.register();
    TalkKey.register();
    ClientConfigCommand.register();
    EntityRendererRegistry.register(ModEntities.LIFT_CAR, LiftCarRenderer::new);
  }

  private static <T extends BlockEntity> void registerAnimated(BlockEntityType<T> type) {
    BlockEntityRendererFactories.register(type, AnimatedDeviceRenderer::new);
  }

  private static void openDevice(MinecraftClient client, OpenDevicePayload payload) {
    BlockPos pos = payload.pos();
    if (client.world.getBlockEntity(pos) instanceof ControlPanelBlockEntity panel) {
      client.setScreen(new ControlPanelScreen(pos, panel, payload.look()));
    } else if (client.world.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
      client.setScreen(new ControllerScreen(pos, controller));
    } else if (client.world.getBlockEntity(pos) instanceof CircuitBoardBlockEntity board) {
      client.setScreen(new CircuitBoardScreen(pos, board));
    } else if (client.world.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
      client.setScreen(new RadioScreen(pos, radio));
    } else if (client.world.getBlockEntity(pos) instanceof BroadcastDeskBlockEntity desk) {
      client.setScreen(new BroadcastDeskScreen(pos, desk));
    } else if (client.world.getBlockState(pos).getBlock() instanceof JunctionBlock) {
      client.setScreen(new JunctionScreen(pos, client.world.getBlockState(pos)));
    } else if (client.world.getBlockState(pos).getBlock() instanceof IntercomBlock) {
      client.setScreen(new IntercomScreen(pos));
    }
  }

  private static void showMainPanel(MinecraftClient client, MainPanelViewPayload payload) {
    if (payload.open()) {
      client.setScreen(new MainPanelScreen(payload));
    } else if (client.currentScreen instanceof MainPanelScreen screen
        && screen.shows(payload.panel())) {
      screen.update(payload);
    }
  }

  private static void showCircuitResult(MinecraftClient client, CircuitResultPayload payload) {
    if (client.currentScreen instanceof CircuitBoardScreen screen && screen.shows(payload.pos())) {
      screen.accept(payload);
    }
  }

  private static void registerColors() {
    Block[] lamps = ModBlocks.lamps();
    BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), lamps);
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.JUNCTION, RenderLayer.getCutout());
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CONTROLLER, RenderLayer.getCutout());
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CIRCUIT_BOARD, RenderLayer.getCutout());
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.CIRCUIT_BLOCK, RenderLayer.getCutout());
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REDSTONE_CELL, RenderLayer.getCutout());
    BlockRenderLayerMap.INSTANCE.putBlocks(
        RenderLayer.getCutout(),
        ModBlocks.SOLAR_PANEL,
        ModBlocks.REDSTONE_GENERATOR,
        ModBlocks.LODESTONE_DYNAMO,
        ModBlocks.MASTER_SWITCH,
        ModBlocks.ALARM,
        ModBlocks.RADIO,
        ModBlocks.SPEAKER,
        ModBlocks.INTERCOM,
        ModBlocks.LIFT,
        ModBlocks.LIFT_FLOOR,
        ModBlocks.RADIO_MAST);
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BROADCAST_DESK, RenderLayer.getTranslucent());
    ColorProviderRegistry.BLOCK.register(
        (state, world, pos, tintIndex) -> {
          if (world == null
              || pos == null
              || !(state.getBlock() instanceof AbstractLampBlock block)
              || !(world.getBlockEntity(block.lampPos(pos, state))
                  instanceof LampBlockEntity lamp)) {
            return OPAQUE | Hue.PLAIN.lampRgb();
          }
          return OPAQUE | dim(lamp.color().lampRgb(), lamp.brightness());
        },
        lamps);
    ColorProviderRegistry.BLOCK.register(
        (state, world, pos, tintIndex) -> OPAQUE | state.get(JunctionBlock.OUTPUT).rgb(),
        ModBlocks.JUNCTION);
    ColorProviderRegistry.ITEM.register(
        (stack, tintIndex) ->
            tintIndex == TINTED_LAYER
                ? OPAQUE | stack.getOrDefault(ModComponents.LAMP_COLOR, Hue.PLAIN).lampRgb()
                : NO_TINT,
        lamps);
    for (Hue hue : Hue.values()) {
      Item wire = ModItems.wire(hue);
      ColorProviderRegistry.ITEM.register(
          (stack, tintIndex) -> tintIndex == TINTED_LAYER ? OPAQUE | hue.rgb() : NO_TINT, wire);
    }
  }

  private static int dim(int rgb, int brightness) {
    int scale = LAMP_TINT_FLOOR + brightness;
    int span = LAMP_TINT_FLOOR + 15;
    int r = ((rgb >> 16) & 0xFF) * scale / span;
    int g = ((rgb >> 8) & 0xFF) * scale / span;
    int b = (rgb & 0xFF) * scale / span;
    return r << 16 | g << 8 | b;
  }
}
