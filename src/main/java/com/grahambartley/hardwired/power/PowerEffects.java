package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireNetwork;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

final class PowerEffects {

  private static final float FUSE_VOLUME = 1.4F;

  private PowerEffects() {}

  static void show(
      ServerWorld world,
      Long2IntOpenHashMap sources,
      Long2LongOpenHashMap starts,
      boolean masterOpen,
      GridState grid,
      int load,
      int supply,
      boolean fuse) {
    int workload = grid.givesPower() ? PowerWear.workload(load, supply) : 0;
    boolean triggered = false;
    for (Long2IntMap.Entry entry : sources.long2IntEntrySet()) {
      BlockPos pos = BlockPos.fromLong(entry.getLongKey());
      if (!world.isChunkLoaded(pos) || !(world.getBlockEntity(pos) instanceof SourceBlockEntity source)) {
        continue;
      }
      boolean running =
          !masterOpen
              && !starts.containsKey(entry.getLongKey())
              && SourceState.byIndex(entry.getIntValue()) == SourceState.READY;
      source.showGrid(running, workload, load, supply, grid);
      if (fuse) {
        world.playSound(null, pos, ModSounds.FUSE_POP, SoundCategory.BLOCKS, FUSE_VOLUME, 1F);
        if (!triggered) {
          ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.BLOWN_FUSE);
          triggered = true;
        }
      }
    }
  }

  static void broughtUp(ServerWorld world, LongOpenHashSet masters, Set<WireNetwork> members) {
    for (long master : masters) {
      BlockPos pos = BlockPos.fromLong(master);
      if (members.stream().anyMatch(network -> network.isTouching(pos))) {
        ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.POWERS_ON);
      }
    }
  }

  static void earned(ServerWorld world, Set<WireNetwork> members) {
    members.stream()
        .flatMap(network -> network.lastSources().keySet().longStream().boxed())
        .map(BlockPos::fromLong)
        .filter(pos -> world.getBlockEntity(pos) instanceof SourceBlockEntity source
            && source.kind() == SourceKind.SOLAR)
        .findFirst()
        .ifPresent(pos -> ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.SOLAR_CHARGED));
  }
}
