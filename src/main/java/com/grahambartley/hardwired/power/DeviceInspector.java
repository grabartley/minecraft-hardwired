package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.network.DeviceInspectPayload;
import com.grahambartley.hardwired.network.DeviceInspectResultPayload;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireViewer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class DeviceInspector {

  private static final double EXTRA_REACH = 1D;

  private DeviceInspector() {}

  public static void inspect(ServerPlayerEntity player, DeviceInspectPayload request) {
    BlockPos pos = request.pos();
    WireEngine engine = WireEngine.of(player.getServerWorld());
    if (!player.canInteractWithBlockAt(pos, EXTRA_REACH) || !engine.isLoaded(pos)) {
      return;
    }
    WireViewer viewer = WireViewer.of(player);
    engine.power().networksAt(pos).stream()
        .filter(viewer::canSee)
        .findFirst()
        .ifPresent(
            network ->
                ServerPlayNetworking.send(
                    player,
                    new DeviceInspectResultPayload(
                        pos,
                        network.gridLoad(),
                        network.gridSupply(),
                        network.gridState() == GridState.OVERLOADED)));
  }
}
