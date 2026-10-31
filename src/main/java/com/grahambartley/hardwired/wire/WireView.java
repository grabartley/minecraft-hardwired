package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import com.grahambartley.hardwired.network.WireViewClearPayload;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class WireView {

  private static final int UPDATE_INTERVAL = 2;
  private static final Map<UUID, Viewer> VIEWERS = new HashMap<>();

  private record Viewer(ServerWorld world, Long2IntOpenHashMap sent) {}

  private WireView() {}

  public static void tick(MinecraftServer server) {
    if (server.getTicks() % UPDATE_INTERVAL != 0) {
      return;
    }
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
      update(player);
    }
  }

  public static void forget(ServerPlayerEntity player) {
    VIEWERS.remove(player.getUuid());
  }

  private static void update(ServerPlayerEntity player) {
    Viewer viewer = VIEWERS.get(player.getUuid());
    if (!WireItem.isHolding(player)) {
      if (viewer != null) {
        VIEWERS.remove(player.getUuid());
        ServerPlayNetworking.send(player, WireViewClearPayload.INSTANCE);
      }
      return;
    }
    if (viewer == null || viewer.world != player.getServerWorld()) {
      if (viewer != null) {
        ServerPlayNetworking.send(player, WireViewClearPayload.INSTANCE);
      }
      viewer = new Viewer(player.getServerWorld(), new Long2IntOpenHashMap());
      viewer.sent.defaultReturnValue(-1);
      VIEWERS.put(player.getUuid(), viewer);
    }
    sendChanges(player, viewer);
  }

  private static void sendChanges(ServerPlayerEntity player, Viewer viewer) {
    WireEngine engine = WireEngine.of(viewer.world);
    int radius = (Hardwired.SERVER_CONFIG.wire().viewRangeBlocks() >> 4) + 1;
    ChunkPos center = player.getChunkPos();
    for (int x = center.x - radius; x <= center.x + radius; x++) {
      for (int z = center.z - radius; z <= center.z + radius; z++) {
        long chunk = ChunkPos.toLong(x, z);
        int version = engine.grid().chunkVersion(chunk);
        if (viewer.sent.get(chunk) != version) {
          viewer.sent.put(chunk, version);
          ServerPlayNetworking.send(player, describe(viewer.world, engine, chunk));
        }
      }
    }
  }

  static WireChunkPayload describe(ServerWorld world, WireEngine engine, long chunk) {
    List<CellView> cells = new ArrayList<>();
    Set<MarkView> marks = new HashSet<>();
    WireGrid grid = engine.grid();
    for (WireCell cell : grid.cellsInChunk(chunk)) {
      WireNetwork network = grid.networkOf(cell);
      boolean endsConnected = false;
      var arms =
          WireTopology.arms(cell.face(), face -> grid.contains(new WireCell(face, cell.color())));
      for (TouchPoint touch : WireTopology.touches(cell.face(), arms)) {
        if (!isInChunk(touch.pos(), chunk)) {
          continue;
        }
        boolean output = WireRules.isOutput(world.getBlockState(touch.pos()));
        boolean input = engine.isInput(touch, cell.color());
        if (input || output) {
          marks.add(new MarkView(touch.pos().asLong(), input));
          endsConnected = true;
        }
      }
      int flags = network.isOn() ? WireChunkPayload.ON : 0;
      flags |= network.isTripped() ? WireChunkPayload.TRIPPED : 0;
      flags |= arms.size() <= 1 && !endsConnected ? WireChunkPayload.LOOSE_END : 0;
      cells.add(
          new CellView(
              cell.face().pos().asLong(),
              cell.face().side().getId(),
              cell.color().ordinal(),
              flags));
    }
    return new WireChunkPayload(chunk, cells, List.copyOf(marks));
  }

  private static boolean isInChunk(BlockPos pos, long chunk) {
    return ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4) == chunk;
  }
}
