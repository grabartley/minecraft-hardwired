package com.grahambartley.hardwired.network.handler;

import com.grahambartley.hardwired.network.ServerConfigPayloads.EditServerConfigC2SPayload;
import com.grahambartley.hardwired.server.ServerConfigEditPermission;
import com.grahambartley.hardwired.server.ServerConfigService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerConfigNetworkHandler {

  private ServerConfigNetworkHandler() {}

  public static void handleEditServerConfig(
      final EditServerConfigC2SPayload payload, final ServerPlayNetworking.Context context) {
    final ServerPlayerEntity player = context.player();
    if (!ServerConfigEditPermission.canEdit(player, player.getServer())) {
      ServerConfigService.sendTo(player);
      return;
    }
    ServerConfigService.update(player.getServer(), payload.config());
  }
}
