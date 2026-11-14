package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.SpanViewPayload;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public final class SpanSync {

  private static final int CHECK_TICKS = 5;
  private static final int CHUNK_BLOCKS = 16;
  private static final Map<UUID, Sent> SENT = new HashMap<>();

  private record Sent(RegistryKey<World> world, List<SpanViewPayload.SpanView> spans) {}

  private SpanSync() {}

  public static void tick(MinecraftServer server) {
    if (server.getTicks() % CHECK_TICKS != 0) {
      return;
    }
    int range = (server.getPlayerManager().getViewDistance() + 1) * CHUNK_BLOCKS;
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
      if (ServerPlayNetworking.canSend(player, SpanViewPayload.ID)) {
        send(player, range);
      }
    }
  }

  public static void forget(ServerPlayerEntity player) {
    SENT.remove(player.getUuid());
  }

  public static void clear() {
    SENT.clear();
  }

  private static void send(ServerPlayerEntity player, int range) {
    WireEngine engine = WireEngine.of(player.getServerWorld());
    WireViewer viewer = WireViewer.of(player);
    boolean holding = WireItem.isHolding(player);
    List<SpanViewPayload.SpanView> spans =
        SpanReach.near(engine.spans(), player.getBlockPos(), range, SpanViewPayload.MOST_SPANS)
            .stream()
            .map(
                span ->
                    SpanViewPayload.SpanView.forViewer(
                        span, viewer, holding, engine.isSpanOn(span)))
            .toList();
    Sent sent = new Sent(player.getServerWorld().getRegistryKey(), spans);
    if (!sent.equals(SENT.get(player.getUuid()))) {
      SENT.put(player.getUuid(), sent);
      ServerPlayNetworking.send(player, new SpanViewPayload(spans));
    }
  }
}
