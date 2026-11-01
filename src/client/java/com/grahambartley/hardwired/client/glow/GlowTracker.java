package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import com.grahambartley.hardwired.light.GlowSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

final class GlowTracker {

  private static final int CHUNK_RADIUS = 4;
  private static final int MAX_GLOWS = 64;
  private static final int CHECK_TICKS = 5;
  private static final int REBUILD_TICKS = 20;

  private GlowClock clock = new GlowClock(CHECK_TICKS, REBUILD_TICKS);
  private GlowMesh mesh = GlowMesh.EMPTY;
  private List<Glow> shown = List.of();
  private ClientWorld world;
  private CompletableFuture<GlowMesh> pending;

  GlowMesh mesh(ClientWorld current, BlockPos center) {
    if (current != world) {
      world = current;
      clock = new GlowClock(CHECK_TICKS, REBUILD_TICKS);
      mesh = GlowMesh.EMPTY;
      shown = List.of();
      pending = null;
    }
    if (pending != null && pending.isDone()) {
      mesh = pending.join();
      pending = null;
    }
    if (pending == null && clock.checkDue(world.getTime())) {
      refresh(center);
    }
    return mesh;
  }

  private void refresh(BlockPos center) {
    List<Glow> glows = GlowSources.nearest(glowsAround(center), center, MAX_GLOWS);
    if (clock.rebuildDue(world.getTime(), !glows.equals(shown))) {
      shown = glows;
      GlowSurfaces surfaces = new WorldGlowSurfaces(world);
      pending =
          CompletableFuture.supplyAsync(
                  () -> GlowMesh.build(glows, surfaces), Util.getMainWorkerExecutor())
              .exceptionally(failure -> GlowMesh.EMPTY);
    }
  }

  private List<Glow> glowsAround(BlockPos center) {
    List<Glow> glows = new ArrayList<>();
    ChunkPos middle = new ChunkPos(center);
    for (int x = middle.x - CHUNK_RADIUS; x <= middle.x + CHUNK_RADIUS; x++) {
      for (int z = middle.z - CHUNK_RADIUS; z <= middle.z + CHUNK_RADIUS; z++) {
        for (BlockEntity entity : world.getChunk(x, z).getBlockEntities().values()) {
          if (entity instanceof GlowSource source) {
            source.glow().ifPresent(glows::add);
          }
        }
      }
    }
    return glows;
  }
}
