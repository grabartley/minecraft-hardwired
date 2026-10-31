package com.grahambartley.hardwired;

import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.LampBlockEntity;
import com.grahambartley.hardwired.client.WireDrag;
import com.grahambartley.hardwired.client.WireHud;
import com.grahambartley.hardwired.client.WireRenderer;
import com.grahambartley.hardwired.client.WireViewStore;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.network.OpenDevicePayload;
import com.grahambartley.hardwired.network.ServerConfigPayloads.SyncServerConfigS2CPayload;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import com.grahambartley.hardwired.network.WireViewClearPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.screen.ControlPanelScreen;
import com.grahambartley.hardwired.screen.JunctionScreen;
import com.grahambartley.hardwired.wire.Hue;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

public class HardwiredClient implements ClientModInitializer {

  private static final int OPAQUE = 0xFF000000;
  private static final int NO_TINT = -1;
  private static final int TINTED_LAYER = 1;
  private static final int LAMP_TINT_FLOOR = 7;

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
        SyncServerConfigS2CPayload.ID,
        (payload, context) -> SyncedServerConfig.apply(payload.config()));
    SyncedServerConfig.resetOnDisconnect();
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WireViewStore.clear());
    ClientTickEvents.END_CLIENT_TICK.register(WireDrag::tick);
    UseBlockCallback.EVENT.register(WireDrag::onUseBlock);
    WorldRenderEvents.LAST.register(WireRenderer::render);
    HudRenderCallback.EVENT.register(WireHud::render);
    registerColors();
  }

  private static void openDevice(MinecraftClient client, OpenDevicePayload payload) {
    BlockPos pos = payload.pos();
    if (client.world.getBlockEntity(pos) instanceof ControlPanelBlockEntity panel) {
      client.setScreen(new ControlPanelScreen(pos, panel));
    } else if (client.world.getBlockState(pos).getBlock() instanceof JunctionBlock) {
      client.setScreen(new JunctionScreen(pos, client.world.getBlockState(pos)));
    }
  }

  private static void registerColors() {
    Block[] lamps = ModBlocks.lamps();
    BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), lamps);
    BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.JUNCTION, RenderLayer.getCutout());
    ColorProviderRegistry.BLOCK.register(
        (state, world, pos, tintIndex) -> {
          if (world == null
              || pos == null
              || !(world.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
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
