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
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
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
  private final Long2LongOpenHashMap startingMasters = new Long2LongOpenHashMap();

  public PowerGrids(ServerWorld world, WireEngine engine, WireGrid grid) {
    this.world = world;
    this.engine = engine;
    this.grid = grid;
    recountAll();
  }

  public void recountAll() {
    dirty.addAll(grid.networks());
  }

  public void changed(BlockPos pos) {
    dirty.addAll(networksAt(pos));
  }

  public void changed(WireNetwork network) {
    dirty.add(network);
  }

  public Set<WireNetwork> networksAt(BlockPos pos) {
    BlockState state = loadedState(pos);
    List<BlockPos> cells =
        state != null && state.getBlock() instanceof WireDevice device
            ? device.cells(pos, state)
            : List.of(pos);
    Set<WireNetwork> found = new LinkedHashSet<>();
    for (BlockPos cell : cells) {
      found.addAll(grid.networksTouching(cell));
    }
    return found;
  }

  private BlockState loadedState(BlockPos pos) {
    return engine.isLoaded(pos) ? world.getBlockState(pos) : null;
  }

  public boolean hasPower(BlockPos pos) {
    return grid.hasPower(pos.asLong());
  }

  public DeviceStatus stopReason(BlockPos pos) {
    GridState best = GridState.NO_SOURCE;
    for (WireNetwork network : networksAt(pos)) {
      best = best.higherOf(network.gridState());
    }
    return best.stopReason();
  }

  public boolean isStarting(BlockPos master) {
    return startingMasters.get(master.asLong()) > world.getTime();
  }

  public void startFrom(BlockPos master) {
    Set<BlockPos> sources = new LinkedHashSet<>();
    for (WireNetwork network : LinkedGroups.reach(networksAt(master), this::neighbours)) {
      network.lastSources().keySet().forEach(source -> sources.add(BlockPos.fromLong(source)));
    }
    Long2LongOpenHashMap scheduled =
        StartOrder.schedule(List.copyOf(sources), master, world.getTime());
    starts.putAll(scheduled);
    if (!scheduled.isEmpty()) {
      startingMasters.put(master.asLong(), scheduled.values().longStream().max().orElseThrow());
    }
    changed(master);
  }

  public void tick() {
    long now = world.getTime();
    starts
        .long2LongEntrySet()
        .removeIf(
            entry -> {
              boolean started = entry.getLongValue() <= now;
              if (started) {
                changed(BlockPos.fromLong(entry.getLongKey()));
              }
              return started;
            });
    if (!dirty.isEmpty()) {
      List<WireNetwork> drained = dirty.stream().filter(grid.networks()::contains).toList();
      dirty.clear();
      recount(drained);
    }
    startingMasters.long2LongEntrySet().removeIf(entry -> entry.getLongValue() <= now);
    if (now % TICKS_PER_SECOND == 0) {
      beat();
    }
  }

  private void beat() {
    List<WireNetwork> waiting = new ArrayList<>();
    for (Set<WireNetwork> group : LinkedGroups.of(grid.networks(), this::neighbours)) {
      if (group.stream().anyMatch(network -> network.gridState().givesPower())) {
        GridBurn.beat(world, engine, group);
      } else if (group.stream().anyMatch(network -> !network.lastSources().isEmpty())) {
        waiting.addAll(group);
      }
    }
    if (!waiting.isEmpty()) {
      recount(waiting);
    }
  }

  private List<WireNetwork> neighbours(WireNetwork network) {
    List<WireNetwork> found = new ArrayList<>();
    for (long link : network.linkedCells()) {
      for (WireNetwork other : grid.networksTouching(link)) {
        if (network.sharesPowerWith(other)) {
          found.add(other);
        }
      }
    }
    return found;
  }

  private void recount(Collection<WireNetwork> starting) {
    Set<WireNetwork> read =
        LinkedGroups.reach(
            starting,
            network -> {
              readFacts(network);
              return neighbours(network);
            });
    LinkedGroups.of(read, this::neighbours).forEach(this::settle);
  }

  private void readFacts(WireNetwork network) {
    Long2IntOpenHashMap loads = network.lastLoads();
    Long2IntOpenHashMap ratings = network.lastRatings();
    Long2IntOpenHashMap sources = network.lastSources();
    network.keepFacts(
        pos ->
            !engine.isLoaded(BlockPos.fromLong(pos)) && network.touchedPositions().contains(pos));
    PowerSettings settings = Hardwired.SERVER_CONFIG.power();
    for (long key : network.touchedPositions()) {
      BlockPos pos = BlockPos.fromLong(key);
      if (!engine.isLoaded(pos)) {
        continue;
      }
      BlockState state = world.getBlockState(pos);
      if (state.getBlock() instanceof WireDevice device) {
        network.link(key, device.cells(pos, state));
        GridTally.addLoad(
            loads, device.loadAnchor(pos, state).asLong(), device.load(world, pos, state));
        if (state.getBlock() instanceof PowerSource source) {
          ratings.put(key, source.sourceKind().rating(settings));
          sources.put(key, sourceState(source, pos, state).ordinal());
        }
        if (state.getBlock() instanceof MasterSwitchBlock master && !master.isClosed(state)) {
          network.masters().add(key);
        }
      } else if (VanillaPower.isComponent(state)) {
        List<BlockPos> cells = VanillaPower.cells(pos, state);
        network.link(key, cells);
        if (network.isOn()) {
          GridTally.addLoad(loads, cells.getFirst().asLong(), settings.loads().vanillaLoad());
        }
      }
    }
  }

  private SourceState sourceState(PowerSource source, BlockPos pos, BlockState state) {
    boolean closed =
        !(world.getBlockEntity(pos) instanceof SourceBlockEntity entity) || entity.isClosed();
    return SourceState.of(closed, source.isFuelled(world, pos, state));
  }

  private void settle(Set<WireNetwork> members) {
    Long2IntOpenHashMap sources = GridTally.sourcesOf(members);
    boolean masterOpen = false;
    long darkUntil = 0;
    boolean wasLive = false;
    GridState before = GridState.NO_SOURCE;
    for (WireNetwork network : members) {
      masterOpen |= !network.masters().isEmpty();
      darkUntil = Math.max(darkUntil, network.darkUntil());
      wasLive |= network.gridState().givesPower();
      before = before.higherOf(network.gridState());
    }
    GridRule.Count count =
        GridTally.count(
            GridTally.loadsOf(members),
            GridTally.ratingsOf(members),
            sources,
            starts.keySet(),
            masterOpen);
    long now = world.getTime();
    GridState next = GridRule.next(count, now, darkUntil);
    GridState previous = wasLive ? GridState.LIVE : before;
    long nextDark = GridRule.darkUntil(previous, next, now, darkUntil);
    int workload = next.givesPower() ? PowerWear.workload(count.load(), count.supply()) : 0;
    for (WireNetwork network : members) {
      boolean powerChanged = network.gridState().givesPower() != next.givesPower();
      boolean stateChanged = network.gridState() != next;
      network.setGridState(next);
      network.setDarkUntil(nextDark);
      network.showTotals(count.load(), count.supply());
      showOnMasters(network, workload, next.givesPower());
      if (powerChanged) {
        engine.pushNetwork(network);
      } else if (stateChanged) {
        grid.markChanged(network);
      }
      if (next == GridState.RETURNING && now < nextDark) {
        dirty.add(network);
      }
    }
    PowerEffects.show(
        world, sources, starts.keySet(), masterOpen, workload, GridRule.blowsFuse(previous, next));
    if (previous != GridState.LIVE && next == GridState.LIVE && !startingMasters.isEmpty()) {
      startingMasters
          .keySet()
          .removeIf(
              (long pos) -> {
                boolean inGrid = members.stream().anyMatch(n -> n.linkedCells().contains(pos));
                if (inGrid) {
                  PowerEffects.broughtUp(world, BlockPos.fromLong(pos));
                }
                return inGrid;
              });
    }
  }

  private void showOnMasters(WireNetwork network, int workload, boolean live) {
    for (long link : network.linkedCells()) {
      BlockPos pos = BlockPos.fromLong(link);
      BlockState state = loadedState(pos);
      if (state != null && state.getBlock() instanceof MasterSwitchBlock master) {
        BlockPos anchor = master.loadAnchor(pos, state);
        if (world.getBlockEntity(anchor) instanceof MasterSwitchBlockEntity entity) {
          entity.showGrid(workload);
          MasterSwitchBlock.showLive(world, anchor, live);
        }
      }
    }
  }
}
