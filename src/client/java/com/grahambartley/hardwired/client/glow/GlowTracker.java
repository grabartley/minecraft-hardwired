package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import com.grahambartley.hardwired.light.GlowSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GlowTracker {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlowTracker.class);
  private static final int CHUNK_RADIUS = 4;
  private static final int MAX_GLOWS = 64;
  private static final int CHECK_TICKS = 5;
  private static final int REBUILD_TICKS = 20;

  private final GlowBuildSlot<GlowMesh> slot = new GlowBuildSlot<>(GlowMesh.EMPTY);
  private final GlowClock clock = new GlowClock(CHECK_TICKS, REBUILD_TICKS);
  private List<Glow> shown = List.of();
  private ClientWorld world;

  GlowMesh mesh(ClientWorld current, BlockPos center) {
    if (current != world) {
      clear();
      world = current;
    }
    GlowMesh mesh = slot.current();
    if (!slot.isBusy() && clock.checkDue(world.getTime())) {
      refresh(center);
    }
    return mesh;
  }

  void clear() {
    world = null;
    clock.reset();
    shown = List.of();
    slot.clear();
  }

  private void refresh(BlockPos center) {
    List<Glow> glows = GlowSources.nearest(glowsAround(center), center, MAX_GLOWS);
    if (clock.rebuildDue(world.getTime(), !glows.equals(shown))) {
      shown = glows;
      GlowSurfaces surfaces = new WorldGlowSurfaces(world);
      slot.build(() -> buildSafely(glows, surfaces), Util.getMainWorkerExecutor());
    }
  }

  private static GlowMesh buildSafely(List<Glow> glows, GlowSurfaces surfaces) {
    try {
      return GlowMesh.build(glows, surfaces);
    } catch (RuntimeException ex) {
      LOGGER.warn("Failed to build cast light: {}", ex.toString());
      return GlowMesh.EMPTY;
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
