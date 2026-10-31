package com.grahambartley.hardwired.wire;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class WireGrid {

  private final Map<WireCell, WireNetwork> cells = new HashMap<>();
  private final Map<WireCell, List<TouchPoint>> cellTouches = new HashMap<>();
  private final Long2ObjectMap<Set<WireCell>> byHost = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Set<WireCell>> byChunk = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Set<WireNetwork>> byTouch = new Long2ObjectOpenHashMap<>();
  private final Long2IntOpenHashMap chunkVersions = new Long2IntOpenHashMap();
  private final Set<WireNetwork> networks = new HashSet<>();
  private final Set<WireNetwork> dirty = new LinkedHashSet<>();

  public boolean contains(WireCell cell) {
    return cells.containsKey(cell);
  }

  public WireNetwork networkOf(WireCell cell) {
    return cells.get(cell);
  }

  public Collection<WireNetwork> networks() {
    return networks;
  }

  public Set<WireCell> cellsOn(BlockPos host) {
    return byHost.getOrDefault(host.asLong(), Set.of());
  }

  public Set<WireCell> cellsInChunk(long chunk) {
    return byChunk.getOrDefault(chunk, Set.of());
  }

  public Set<WireNetwork> networksTouching(BlockPos pos) {
    return networksTouching(pos.asLong());
  }

  public Set<WireNetwork> networksTouching(long pos) {
    return byTouch.getOrDefault(pos, Set.of());
  }

  public boolean isTouched() {
    return !byTouch.isEmpty();
  }

  public boolean isPowered(long pos) {
    for (WireNetwork network : networksTouching(pos)) {
      if (network.isOn()) {
        return true;
      }
    }
    return false;
  }

  public int chunkVersion(long chunk) {
    return chunkVersions.get(chunk);
  }

  public void markChanged(WireNetwork network) {
    for (long chunk : network.chunks()) {
      chunkVersions.addTo(chunk, 1);
    }
  }

  public void markDirty(WireNetwork network) {
    dirty.add(network);
  }

  public List<WireNetwork> drainDirty() {
    List<WireNetwork> drained = new ArrayList<>(dirty);
    dirty.clear();
    return drained;
  }

  public int sizeAfterAdding(WireCell cell) {
    int size = 1;
    for (WireNetwork network : adjacentNetworks(cell)) {
      size += network.size();
    }
    return size;
  }

  public WireNetwork add(WireCell cell) {
    WireNetwork existing = cells.get(cell);
    if (existing != null) {
      return existing;
    }
    Set<WireNetwork> adjacent = adjacentNetworks(cell);
    WireNetwork target = adjacent.isEmpty() ? newNetwork(cell.color()) : largest(adjacent);
    for (WireNetwork other : adjacent) {
      if (other != target) {
        absorb(target, other);
      }
    }
    place(cell, target);
    refreshAround(cell);
    wireChanged(target);
    return target;
  }

  public void remove(WireCell cell) {
    WireNetwork network = cells.get(cell);
    if (network == null) {
      return;
    }
    List<WireCell> adjacent = adjacentCells(cell);
    clearTouches(cell, network);
    cells.remove(cell);
    removeFromIndex(byHost, hostKey(cell), cell);
    removeFromIndex(byChunk, chunkKey(cell), cell);
    leave(cell, network);
    chunkVersions.addTo(chunkKey(cell), 1);
    adjacent.forEach(this::refreshTouches);
    if (network.cells.isEmpty()) {
      networks.remove(network);
      dirty.remove(network);
      return;
    }
    splitIfNeeded(network, adjacent);
  }

  private void wireChanged(WireNetwork network) {
    network.setTripped(false);
    network.tripDetector().reset();
    dirty.add(network);
    markChanged(network);
  }

  private void splitIfNeeded(WireNetwork network, List<WireCell> starts) {
    Set<WireCell> visited = new HashSet<>();
    collect(starts.get(0), visited);
    for (WireCell start : starts) {
      if (!visited.contains(start)) {
        Set<WireCell> part = new HashSet<>();
        collect(start, part);
        visited.addAll(part);
        split(network, part);
      }
    }
    wireChanged(network);
  }

  private void split(WireNetwork network, Set<WireCell> part) {
    WireNetwork fresh = newNetwork(network.color());
    fresh.setOn(network.isOn());
    fresh.setLook(network.look());
    fresh.rememberedInputs().addAll(network.rememberedInputs());
    for (WireCell cell : part) {
      move(cell, network, fresh);
    }
    wireChanged(fresh);
  }

  private void collect(WireCell start, Set<WireCell> visited) {
    Deque<WireCell> queue = new ArrayDeque<>();
    queue.add(start);
    visited.add(start);
    while (!queue.isEmpty()) {
      for (WireCell next : adjacentCells(queue.poll())) {
        if (visited.add(next)) {
          queue.add(next);
        }
      }
    }
  }

  private void absorb(WireNetwork target, WireNetwork other) {
    for (WireCell cell : List.copyOf(other.cells)) {
      move(cell, other, target);
    }
    target.setOn(target.isOn() || other.isOn());
    if (target.look() == null) {
      target.setLook(other.look());
    }
    target.rememberedInputs().addAll(other.rememberedInputs());
    networks.remove(other);
    dirty.remove(other);
  }

  private void move(WireCell cell, WireNetwork from, WireNetwork to) {
    clearTouches(cell, from);
    leave(cell, from);
    join(cell, to);
    addTouches(cell, to);
  }

  private void join(WireCell cell, WireNetwork network) {
    cells.put(cell, network);
    network.cells.add(cell);
    network.chunks.addTo(chunkKey(cell), 1);
  }

  private void leave(WireCell cell, WireNetwork network) {
    long chunk = chunkKey(cell);
    network.cells.remove(cell);
    if (network.chunks.addTo(chunk, -1) == 1) {
      network.chunks.remove(chunk);
    }
  }

  private void place(WireCell cell, WireNetwork network) {
    join(cell, network);
    byHost.computeIfAbsent(hostKey(cell), key -> new HashSet<>()).add(cell);
    byChunk.computeIfAbsent(chunkKey(cell), key -> new HashSet<>()).add(cell);
  }

  private void refreshAround(WireCell cell) {
    refreshTouches(cell);
    adjacentCells(cell).forEach(this::refreshTouches);
  }

  private void refreshTouches(WireCell cell) {
    WireNetwork network = cells.get(cell);
    clearTouches(cell, network);
    addTouches(cell, network);
  }

  private void addTouches(WireCell cell, WireNetwork network) {
    List<TouchPoint> touches =
        WireTopology.touches(
            cell.face(),
            WireTopology.arms(
                cell.face(), face -> cells.containsKey(new WireCell(face, cell.color()))));
    cellTouches.put(cell, touches);
    for (TouchPoint touch : touches) {
      network.touches.addTo(touch, 1);
      long key = touch.pos().asLong();
      if (network.touchedPositions.addTo(key, 1) == 0) {
        byTouch.computeIfAbsent(key, ignored -> new HashSet<>()).add(network);
      }
    }
  }

  private void clearTouches(WireCell cell, WireNetwork network) {
    List<TouchPoint> touches = cellTouches.remove(cell);
    if (touches == null) {
      return;
    }
    for (TouchPoint touch : touches) {
      if (network.touches.addTo(touch, -1) == 1) {
        network.touches.removeInt(touch);
      }
      long key = touch.pos().asLong();
      if (network.touchedPositions.addTo(key, -1) == 1) {
        network.touchedPositions.remove(key);
        unlinkTouch(key, network);
      }
    }
  }

  private void unlinkTouch(long key, WireNetwork network) {
    Set<WireNetwork> touching = byTouch.get(key);
    if (touching != null) {
      touching.remove(network);
      if (touching.isEmpty()) {
        byTouch.remove(key);
      }
    }
  }

  private Set<WireNetwork> adjacentNetworks(WireCell cell) {
    Set<WireNetwork> adjacent = new HashSet<>();
    for (WireCell neighbour : adjacentCells(cell)) {
      adjacent.add(cells.get(neighbour));
    }
    return adjacent;
  }

  private List<WireCell> adjacentCells(WireCell cell) {
    List<WireCell> adjacent = new ArrayList<>(4);
    for (WireFace face : WireTopology.neighbours(cell.face())) {
      WireCell neighbour = new WireCell(face, cell.color());
      if (cells.containsKey(neighbour)) {
        adjacent.add(neighbour);
      }
    }
    return adjacent;
  }

  private WireNetwork newNetwork(Hue color) {
    WireNetwork network = new WireNetwork(color);
    networks.add(network);
    return network;
  }

  private static WireNetwork largest(Set<WireNetwork> networks) {
    WireNetwork largest = null;
    for (WireNetwork network : networks) {
      if (largest == null || network.size() > largest.size()) {
        largest = network;
      }
    }
    return largest;
  }

  private static long hostKey(WireCell cell) {
    return cell.face().pos().asLong();
  }

  private static long chunkKey(WireCell cell) {
    return ChunkPos.toLong(cell.face().pos().getX() >> 4, cell.face().pos().getZ() >> 4);
  }

  private static void removeFromIndex(
      Long2ObjectMap<Set<WireCell>> index, long key, WireCell cell) {
    Set<WireCell> entries = index.get(key);
    if (entries != null) {
      entries.remove(cell);
      if (entries.isEmpty()) {
        index.remove(key);
      }
    }
  }
}
