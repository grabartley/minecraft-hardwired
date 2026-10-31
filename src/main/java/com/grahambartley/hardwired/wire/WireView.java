package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireChunkPayload;
import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import com.grahambartley.hardwired.network.WireViewClearPayload;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.util.math.ChunkPos;

public final class WireView {

  private static final int UPDATE_INTERVAL = 2;
  private static final Map<UUID, Viewer> VIEWERS = new HashMap<>();
  private static final Comparator<CellView> CELL_ORDER =
      Comparator.comparingLong(CellView::pos)
          .thenComparingInt(CellView::side)
          .thenComparingInt(CellView::color)
          .thenComparingInt(CellView::flags)
          .thenComparingInt(CellView::arms);
  private static final Comparator<MarkView> MARK_ORDER =
      Comparator.comparingLong(MarkView::pos).thenComparing(MarkView::input);

  private record Viewer(
      ServerWorld world,
      WireViewer access,
      Long2IntOpenHashMap sent,
      Long2ObjectOpenHashMap<WireChunkPayload> shown) {}

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
    WireViewer access = WireViewer.of(player);
    if (viewer == null
        || viewer.world != player.getServerWorld()
        || !viewer.access.equals(access)) {
      if (viewer != null) {
        ServerPlayNetworking.send(player, WireViewClearPayload.INSTANCE);
      }
      viewer =
          new Viewer(
              player.getServerWorld(),
              access,
              new Long2IntOpenHashMap(),
              new Long2ObjectOpenHashMap<>());
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
          sendIfDifferent(player, viewer, describe(viewer.world, engine, chunk, viewer.access));
        }
      }
    }
    forgetChunksOutOfRange(player, viewer, center, radius);
  }

  private static void sendIfDifferent(
      ServerPlayerEntity player, Viewer viewer, WireChunkPayload payload) {
    WireChunkPayload last = viewer.shown.get(payload.chunk());
    boolean empty = payload.cells().isEmpty();
    if (empty ? last == null : payload.equals(last)) {
      return;
    }
    if (empty) {
      viewer.shown.remove(payload.chunk());
    } else {
      viewer.shown.put(payload.chunk(), payload);
    }
    ServerPlayNetworking.send(player, payload);
  }

  private static void forgetChunksOutOfRange(
      ServerPlayerEntity player, Viewer viewer, ChunkPos center, int radius) {
    for (long chunk : viewer.sent.keySet().toLongArray()) {
      ChunkPos pos = new ChunkPos(chunk);
      if (Math.abs(pos.x - center.x) > radius || Math.abs(pos.z - center.z) > radius) {
        viewer.sent.remove(chunk);
        if (viewer.shown.remove(chunk) != null) {
          ServerPlayNetworking.send(player, new WireChunkPayload(chunk, List.of(), List.of()));
        }
      }
    }
  }

  static WireChunkPayload describe(
      ServerWorld world, WireEngine engine, long chunk, WireViewer access) {
    List<CellView> cells = new ArrayList<>();
    Set<MarkView> marks = new HashSet<>();
    WireGrid grid = engine.grid();
    for (WireCell cell : grid.cellsInChunk(chunk)) {
      if (!access.canSee(cell)) {
        continue;
      }
      WireNetwork network = grid.networkOf(cell);
      boolean endsConnected = false;
      var arms = WireTopology.arms(cell.face(), face -> grid.contains(cell.onFace(face)));
      for (TouchPoint touch : WireTopology.touches(cell.face(), arms)) {
        if (!engine.canRead(touch.pos())) {
          endsConnected = true;
          continue;
        }
        boolean output = WireRules.isOutput(world.getBlockState(touch.pos()), touch);
        boolean input = engine.isInput(touch, cell.color());
        if (input || output) {
          marks.add(new MarkView(touch.pos().asLong(), input));
          endsConnected = true;
        }
      }
      int flags = network.isOn() ? WireChunkPayload.ON : 0;
      flags |= network.isTripped() ? WireChunkPayload.TRIPPED : 0;
      flags |= arms.size() <= 1 && !endsConnected ? WireChunkPayload.LOOSE_END : 0;
      flags |= cell.isSealed() ? WireChunkPayload.SEALED : 0;
      cells.add(
          new CellView(
              cell.face().pos().asLong(),
              cell.face().side().getId(),
              cell.color().ordinal(),
              flags,
              CellView.armMask(arms)));
    }
    cells.sort(CELL_ORDER);
    List<MarkView> sortedMarks = new ArrayList<>(marks);
    sortedMarks.sort(MARK_ORDER);
    return new WireChunkPayload(chunk, cells, sortedMarks);
  }
}
