package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

final class GridBurn {

  private GridBurn() {}

  static void beat(ServerWorld world, WireEngine engine, Set<WireNetwork> members) {
    Long2IntOpenHashMap ratings = GridTally.ratingsOf(members);
    List<SourceBlockEntity> loaded = new ArrayList<>();
    List<SourceBlockEntity> running = new ArrayList<>();
    List<BurnPlan.Source> plan = new ArrayList<>();
    for (Long2IntMap.Entry entry : ratings.long2IntEntrySet()) {
      BlockPos pos = BlockPos.fromLong(entry.getLongKey());
      if (engine.isLoaded(pos) && world.getBlockEntity(pos) instanceof SourceBlockEntity source) {
        loaded.add(source);
        if (source.isRunning()) {
          running.add(source);
          plan.add(new BurnPlan.Source(source.kind(), entry.getIntValue()));
        }
      }
    }
    int load = GridTally.total(GridTally.loadsOf(members));
    BurnPlan burn = BurnPlan.forSecond(plan, load, Hardwired.SERVER_CONFIG.power().burn());
    for (int i = 0; i < running.size(); i++) {
      if (running.get(i) instanceof ChargeStore store) {
        store.burn(burn.burnFor(i));
      }
    }
    if (burn.earned() > 0) {
      share(world, burn.earned(), loaded, running);
    }
  }

  private static void share(
      ServerWorld world,
      double earned,
      List<SourceBlockEntity> loaded,
      List<SourceBlockEntity> running) {
    List<ChargeStore> earners = new ArrayList<>();
    for (SourceBlockEntity source : loaded) {
      if (source instanceof ChargeStore store
          && store.acceptsEarnings()
          && store.charge() < store.capacity()) {
        earners.add(store);
      }
    }
    if (!earners.isEmpty()) {
      double share = earned / earners.size();
      earners.forEach(store -> store.earn(share));
      PowerEffects.earned(world, running);
    }
  }
}
