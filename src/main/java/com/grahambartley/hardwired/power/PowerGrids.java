package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.MasterSwitchBlock;
import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.wire.VanillaPower;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireGrid;
import com.grahambartley.hardwired.wire.WireNetwork;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class PowerGrids {

  private static final int TICKS_PER_SECOND = 20;

  private final ServerWorld world;
  private final WireEngine engine;
  private final WireGrid grid;
  private final Set<WireNetwork> dirty = new LinkedHashSet<>();
  private final Long2LongOpenHashMap starts = new Long2LongOpenHashMap();
  private final LongOpenHashSet startingMasters = new LongOpenHashSet();

  public PowerGrids(ServerWorld world, WireEngine engine, WireGrid grid) {
    this.world = world;
    this.engine = engine;
    this.grid = grid;
    dirty.addAll(grid.networks());
  }

  public void changed(BlockPos pos) {
    dirty.addAll(grid.networksTouching(pos));
  }

  public void changed(WireNetwork network) {
    dirty.add(network);
  }

  public boolean hasPower(BlockPos pos) {
    return grid.hasPower(pos.asLong());
  }

  public DeviceStatus stopReason(BlockPos pos) {
    GridState best = GridState.NO_SOURCE;
    for (WireNetwork network : grid.networksTouching(pos)) {
      if (network.gridState().ordinal() > best.ordinal()) {
        best = network.gridState();
      }
    }
    return best.stopReason();
  }

  public boolean isStarting(BlockPos master) {
    return !starts.isEmpty() && startingMasters.contains(master.asLong());
  }

  public void startFrom(BlockPos master) {
    List<BlockPos> sources = new ArrayList<>();
    for (WireNetwork network : grid.networksTouching(master)) {
      for (WireNetwork member : members(network)) {
        for (long source : member.lastSources().keySet()) {
          BlockPos pos = BlockPos.fromLong(source);
          if (!sources.contains(pos)) {
            sources.add(pos);
          }
        }
      }
    }
    Long2LongOpenHashMap scheduled = StartOrder.schedule(sources, master, world.getTime());
    starts.putAll(scheduled);
    if (!scheduled.isEmpty()) {
      startingMasters.add(master.asLong());
    }
    changed(master);
  }

  public void tick() {
    long now = world.getTime();
    if (starts.long2LongEntrySet().removeIf(entry -> entry.getLongValue() <= now)) {
      markEverySource();
    }
    if (!dirty.isEmpty()) {
      List<WireNetwork> drained = List.copyOf(dirty);
      dirty.clear();
      Set<WireNetwork> seen = new HashSet<>();
      for (WireNetwork network : drained) {
        if (grid.networks().contains(network) && !seen.contains(network)) {
          recount(members(network), seen);
        }
      }
    }
    if (now % TICKS_PER_SECOND == 0) {
      beat();
    }
  }

  private void markEverySource() {
    for (WireNetwork network : grid.networks()) {
      if (!network.lastSources().isEmpty()) {
        dirty.add(network);
      }
    }
  }

  private void beat() {
    Set<WireNetwork> seen = new HashSet<>();
    for (WireNetwork network : List.copyOf(grid.networks())) {
      if (seen.contains(network)) {
        continue;
      }
      Set<WireNetwork> members = members(network);
      if (network.gridState().givesPower()) {
        seen.addAll(members);
        burn(members);
      } else if (members.stream().anyMatch(member -> !member.lastSources().isEmpty())) {
        recount(members, seen);
      } else {
        seen.addAll(members);
      }
    }
  }

  private Set<WireNetwork> members(WireNetwork start) {
    Set<WireNetwork> found = new LinkedHashSet<>();
    Deque<WireNetwork> queue = new ArrayDeque<>();
    found.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      WireNetwork network = queue.poll();
      for (long link : network.links()) {
        for (WireNetwork other : grid.networksTouching(link)) {
          if (network.sharesPowerWith(other) && found.add(other)) {
            queue.add(other);
          }
        }
      }
    }
    return found;
  }

  private void recount(Set<WireNetwork> members, Set<WireNetwork> seen) {
    for (WireNetwork network : members) {
      readFacts(network);
    }
    Set<WireNetwork> linked = new LinkedHashSet<>(members);
    for (WireNetwork network : members) {
      linked.addAll(members(network));
    }
    seen.addAll(linked);
    settle(linked);
  }

  private void readFacts(WireNetwork network) {
    Long2IntOpenHashMap loads = network.lastLoads();
    Long2IntOpenHashMap ratings = network.lastRatings();
    Long2IntOpenHashMap sources = network.lastSources();
    keepUnloaded(loads);
    keepUnloaded(ratings);
    keepUnloaded(sources);
    network.links().removeIf((long pos) -> engine.isLoaded(BlockPos.fromLong(pos)));
    network.masters().removeIf((long pos) -> engine.isLoaded(BlockPos.fromLong(pos)));
    PowerSettings settings = Hardwired.SERVER_CONFIG.power();
    for (long key : network.touchedPositions()) {
      BlockPos pos = BlockPos.fromLong(key);
      if (!engine.isLoaded(pos)) {
        continue;
      }
      BlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof WireDevice device) {
        network.links().add(key);
        loads.mergeInt(
            device.loadAnchor(pos, state).asLong(), device.load(world, pos, state), Math::max);
        if (state.getBlock() instanceof PowerSource source) {
          ratings.put(key, source.sourceKind().rating(settings));
          sources.put(key, sourceState(source, pos, state).ordinal());
        }
        if (state.getBlock() instanceof MasterSwitchBlock master && !master.isClosed(state)) {
          network.masters().add(key);
        }
      } else if (VanillaPower.isComponent(state)) {
        network.links().add(key);
        if (network.isOn()) {
          loads.mergeInt(
              VanillaPower.loadAnchor(pos, state).asLong(), settings.vanillaLoad(), Math::max);
        }
      }
    }
  }

  private void keepUnloaded(Long2IntOpenHashMap facts) {
    facts.keySet().removeIf((long pos) -> engine.isLoaded(BlockPos.fromLong(pos)));
  }

  private SourceState sourceState(PowerSource source, BlockPos pos, BlockState state) {
    boolean closed =
        !(world.getBlockEntity(pos) instanceof SourceBlockEntity entity) || entity.isClosed();
    return SourceState.of(closed, source.isFuelled(world, pos, state));
  }

  private void settle(Set<WireNetwork> members) {
    Long2IntOpenHashMap loads = new Long2IntOpenHashMap();
    Long2IntOpenHashMap ratings = new Long2IntOpenHashMap();
    Long2IntOpenHashMap sources = new Long2IntOpenHashMap();
    boolean masterOpen = false;
    long darkUntil = 0;
    boolean wasLive = false;
    GridState before = GridState.NO_SOURCE;
    for (WireNetwork network : members) {
      network
          .lastLoads()
          .long2IntEntrySet()
          .fastForEach(e -> loads.mergeInt(e.getLongKey(), e.getIntValue(), Math::max));
      ratings.putAll(network.lastRatings());
      sources.putAll(network.lastSources());
      masterOpen |= !network.masters().isEmpty();
      darkUntil = Math.max(darkUntil, network.darkUntil());
      wasLive |= network.gridState().givesPower();
      before = network.gridState().ordinal() > before.ordinal() ? network.gridState() : before;
    }
    int load = loads.values().intStream().sum();
    int supply = 0;
    int switchedOff = 0;
    boolean starting = false;
    for (Long2IntMap.Entry entry : sources.long2IntEntrySet()) {
      SourceState state = SourceState.byIndex(entry.getIntValue());
      boolean waiting = starts.containsKey(entry.getLongKey());
      starting |= waiting;
      if (masterOpen || state == SourceState.SWITCHED_OFF) {
        switchedOff++;
      } else if (state == SourceState.READY && !waiting) {
        supply += ratings.get(entry.getLongKey());
      }
    }
    GridRule.Count count = new GridRule.Count(load, supply, sources.size(), switchedOff, starting);
    long now = world.getTime();
    GridState next = GridRule.next(count, now, darkUntil);
    GridState previous = wasLive ? GridState.LIVE : before;
    long nextDark = GridRule.darkUntil(previous, next, now, darkUntil);
    boolean fuse = GridRule.blowsFuse(previous, next);
    for (WireNetwork network : members) {
      boolean powerChanged = network.gridState().givesPower() != next.givesPower();
      boolean stateChanged = network.gridState() != next;
      network.setGridState(next);
      network.setDarkUntil(nextDark);
      network.showTotals(load, supply);
      showOnMasters(network, load, supply);
      if (powerChanged) {
        engine.pushNetwork(network);
      } else if (stateChanged) {
        grid.markChanged(network);
      }
      if (next == GridState.RETURNING && now < nextDark) {
        dirty.add(network);
      }
    }
    PowerEffects.show(world, sources, starts, masterOpen, next, load, supply, fuse);
    if (previous != GridState.LIVE && next == GridState.LIVE && !startingMasters.isEmpty()) {
      PowerEffects.broughtUp(world, startingMasters, members);
      startingMasters.removeIf(
          (long pos) -> members.stream().anyMatch(n -> n.isTouching(BlockPos.fromLong(pos))));
    }
  }

  private void showOnMasters(WireNetwork network, int load, int supply) {
    for (long link : network.links()) {
      BlockPos pos = BlockPos.fromLong(link);
      if (engine.isLoaded(pos)
          && world.getBlockState(pos).getBlock() instanceof MasterSwitchBlock master) {
        BlockPos anchor = master.loadAnchor(pos, world.getBlockState(pos));
        if (world.getBlockEntity(anchor) instanceof MasterSwitchBlockEntity entity) {
          entity.showGrid(load, supply);
        }
      }
    }
  }

  private void burn(Set<WireNetwork> members) {
    Long2IntOpenHashMap loads = new Long2IntOpenHashMap();
    Long2IntOpenHashMap ratings = new Long2IntOpenHashMap();
    for (WireNetwork network : members) {
      network
          .lastLoads()
          .long2IntEntrySet()
          .fastForEach(e -> loads.mergeInt(e.getLongKey(), e.getIntValue(), Math::max));
      ratings.putAll(network.lastRatings());
    }
    List<SourceBlockEntity> running = new ArrayList<>();
    List<BurnPlan.Source> plan = new ArrayList<>();
    for (Long2IntMap.Entry entry : ratings.long2IntEntrySet()) {
      BlockPos pos = BlockPos.fromLong(entry.getLongKey());
      if (engine.isLoaded(pos)
          && world.getBlockEntity(pos) instanceof SourceBlockEntity source
          && source.isRunning()) {
        running.add(source);
        plan.add(new BurnPlan.Source(source.kind(), entry.getIntValue()));
      }
    }
    int load = loads.values().intStream().sum();
    BurnPlan burn =
        BurnPlan.forSecond(plan, load, Hardwired.SERVER_CONFIG.power().chargePerLoadMinute());
    List<ChargeStore> earners = new ArrayList<>();
    for (int i = 0; i < running.size(); i++) {
      SourceBlockEntity source = running.get(i);
      if (source instanceof ChargeStore store) {
        store.burn(burn.burnFor(i));
      }
    }
    for (WireNetwork network : members) {
      for (long pos : network.touchedPositions()) {
        BlockPos at = BlockPos.fromLong(pos);
        if (engine.isLoaded(at)
            && world.getBlockEntity(at) instanceof ChargeStore store
            && store.acceptsEarnings()
            && store.charge() < store.capacity()
            && !earners.contains(store)) {
          earners.add(store);
        }
      }
    }
    if (burn.earned() > 0 && !earners.isEmpty()) {
      double share = burn.earned() / earners.size();
      earners.forEach(store -> store.earn(share));
      PowerEffects.earned(world, members);
    }
  }
}
