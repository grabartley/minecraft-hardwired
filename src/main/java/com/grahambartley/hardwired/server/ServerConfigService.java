package com.grahambartley.hardwired.server;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.ConfigPaths;
import com.grahambartley.hardwired.config.HardwiredConfig;
import com.grahambartley.hardwired.network.ServerConfigPayloads.SyncServerConfigS2CPayload;
import java.nio.file.Path;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerConfigService {
  public static final int OP_PERMISSION_LEVEL = 2;

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfigService.class);

  private ServerConfigService() {}

  public static void loadFromSession(final LevelStorage.Session session) {
    if (session == null) {
      Hardwired.SERVER_CONFIG = HardwiredConfig.defaults();
      return;
    }
    loadFromPath(session.getDirectory(WorldSavePath.ROOT).normalize());
  }

  public static void loadFromWorld(final MinecraftServer server) {
    if (server == null) {
      Hardwired.SERVER_CONFIG = HardwiredConfig.defaults();
      return;
    }
    loadFromPath(worldRoot(server));
  }

  private static void loadFromPath(final Path worldRoot) {
    final ConfigPaths paths = new ConfigPaths(worldRoot);
    Hardwired.SERVER_CONFIG = HardwiredConfig.loadOrCreate(paths.getServerConfigPath());
    LOGGER.info("Hardwired server config loaded for {}", worldRoot.getFileName());
  }

  public static boolean update(final MinecraftServer server, final HardwiredConfig updated) {
    if (server == null || updated == null) {
      return false;
    }
    final ConfigPaths paths = new ConfigPaths(worldRoot(server));
    if (!HardwiredConfig.save(paths.getServerConfigPath(), updated)) {
      return false;
    }
    Hardwired.SERVER_CONFIG = updated;
    broadcast(server, updated);
    return true;
  }

  public static void sendTo(final ServerPlayerEntity player) {
    ServerPlayNetworking.send(player, new SyncServerConfigS2CPayload(Hardwired.SERVER_CONFIG));
  }

  public static void broadcast(final MinecraftServer server, final HardwiredConfig config) {
    if (server == null) {
      return;
    }
    final SyncServerConfigS2CPayload payload = new SyncServerConfigS2CPayload(config);
    for (final ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  private static Path worldRoot(final MinecraftServer server) {
    return server.getSavePath(WorldSavePath.ROOT).normalize();
  }
}
