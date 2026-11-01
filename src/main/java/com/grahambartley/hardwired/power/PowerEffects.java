package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

final class PowerEffects {

  private static final float FUSE_VOLUME = 1.4F;

  private PowerEffects() {}

  static void show(
      ServerWorld world,
      Long2IntOpenHashMap sources,
      LongSet waiting,
      boolean masterOpen,
      int workload,
      boolean fuse) {
    for (Long2IntMap.Entry entry : sources.long2IntEntrySet()) {
      BlockPos pos = BlockPos.fromLong(entry.getLongKey());
      if (!WireEngine.of(world).isLoaded(pos)
          || !(world.getBlockEntity(pos) instanceof SourceBlockEntity source)) {
        continue;
      }
      SourceState state = SourceState.byIndex(entry.getIntValue());
      boolean delayed = waiting.contains(entry.getLongKey());
      source.showGrid(GridTally.runs(state, delayed, masterOpen), workload);
      if (fuse) {
        world.playSound(null, pos, ModSounds.FUSE_POP, SoundCategory.BLOCKS, FUSE_VOLUME, 1F);
        ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.BLOWN_FUSE);
      }
    }
  }

  static void broughtUp(ServerWorld world, BlockPos master) {
    ModCriteria.DEVICE.triggerNear(world, master, ModCriteria.POWERS_ON);
  }

  static void earned(ServerWorld world, List<SourceBlockEntity> running) {
    running.stream()
        .filter(source -> source.kind() == SourceKind.SOLAR)
        .forEach(
            source ->
                ModCriteria.DEVICE.triggerNear(world, source.getPos(), ModCriteria.SOLAR_CHARGED));
  }

  static void stopped(ServerWorld world, WireEngine engine, Collection<WireNetwork> members) {
    for (WireNetwork network : members) {
      for (long touched : network.touchedPositions()) {
        BlockPos pos = BlockPos.fromLong(touched);
        if (engine.isLoaded(pos) && wantsToRun(world, pos)) {
          ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.LIGHTS_OUT);
        }
      }
    }
  }

  private static boolean wantsToRun(ServerWorld world, BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    return state.getBlock() instanceof WireDevice device && device.load(world, pos, state) > 0;
  }
}
