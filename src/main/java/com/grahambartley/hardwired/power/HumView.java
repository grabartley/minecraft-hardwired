package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.network.HumLevelPayload;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireGrid;
import com.grahambartley.hardwired.wire.WireNetwork;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class HumView {

  private static final int INTERVAL = 20;
  private static final int CHUNK_SHIFT = 4;

  private static final Map<UUID, Integer> SENT = new HashMap<>();

  private HumView() {}

  public static void tick(MinecraftServer server) {
    if (server.getTicks() % INTERVAL != 0) {
      return;
    }
    int range = Hardwired.SERVER_CONFIG.power().hum().humRange();
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
      int load = loadNear(player, range);
      Integer last = SENT.put(player.getUuid(), load);
      if (load > 0 || (last != null && last > 0)) {
        ServerPlayNetworking.send(player, new HumLevelPayload(load));
      }
    }
  }

  public static void forget(ServerPlayerEntity player) {
    SENT.remove(player.getUuid());
  }

  private static int loadNear(ServerPlayerEntity player, int range) {
    WireGrid grid = WireEngine.of(player.getServerWorld()).grid();
    ChunkPos center = player.getChunkPos();
    int radius = (range >> CHUNK_SHIFT) + 1;
    Set<WireNetwork> near = new HashSet<>();
    for (int x = center.x - radius; x <= center.x + radius; x++) {
      for (int z = center.z - radius; z <= center.z + radius; z++) {
        for (WireCell cell : grid.cellsInChunk(ChunkPos.toLong(x, z))) {
          near.add(grid.networkOf(cell));
        }
      }
    }
    BlockPos at = player.getBlockPos();
    long reach = (long) range * range;
    Long2IntOpenHashMap counted = new Long2IntOpenHashMap();
    for (WireNetwork network : near) {
      if (network.gridState().givesPower()) {
        for (Long2IntMap.Entry entry : network.lastLoads().long2IntEntrySet()) {
          if (BlockPos.fromLong(entry.getLongKey()).getSquaredDistance(at) <= reach) {
            GridTally.addLoad(counted, entry.getLongKey(), entry.getIntValue());
          }
        }
      }
    }
    return GridTally.total(counted);
  }
}
