package com.grahambartley.hardwired;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.command.HardwiredCommand;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.device.CircuitActions;
import com.grahambartley.hardwired.device.IntercomActions;
import com.grahambartley.hardwired.network.ModNetworking;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.server.ServerConfigService;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireSealing;
import com.grahambartley.hardwired.wire.WireView;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hardwired implements ModInitializer {

  public static final String MOD_ID = "hardwired";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static volatile HardwiredConfig SERVER_CONFIG = HardwiredConfig.defaults();

  @Override
  public void onInitialize() {
    ModComponents.register();
    ModBlocks.register();
    ModBlockEntities.register();
    ModItems.register();
    ModNetworking.register();
    ModCriteria.register();
    ServerLifecycleEvents.SERVER_STARTING.register(ServerConfigService::loadFromWorld);
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> ServerConfigService.sendTo(handler.getPlayer()));
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registries, environment) -> HardwiredCommand.register(dispatcher));
    UseBlockCallback.EVENT.register(WireSealing::use);
    ServerTickEvents.END_WORLD_TICK.register(world -> WireEngine.of(world).tick());
    ServerTickEvents.END_SERVER_TICK.register(WireView::tick);
    ServerChunkEvents.CHUNK_LOAD.register(
        (world, chunk) -> WireEngine.of(world).onChunkLoaded(chunk.getPos()));
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> {
          WireView.forget(handler.getPlayer());
          CircuitActions.forget(handler.getPlayer());
          IntercomActions.forget(handler.getPlayer());
        });
    LOGGER.info("Hardwired initialised");
  }
}
