package com.grahambartley.hardwired.wire;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

public final class WireGrid {

  private final Map<WireCell, WireNetwork> cells = new HashMap<>();
  private final Map<WireCell, List<TouchPoint>> cellTouches = new HashMap<>();
  private final Long2ObjectMap<Set<WireCell>> byHost = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Set<WireCell>> byChunk = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectMap<Set<WireNetwork>> byTouch = new Long2ObjectOpenHashMap<>();
  private final Long2IntOpenHashMap chunkVersions = new Long2IntOpenHashMap();
  private final Set<WireNetwork> networks = new HashSet<>();
  private final WireSpans spans = new WireSpans();
  private final Set<WireNetwork> dirty = new LinkedHashSet<>();
  private final LongOpenHashSet touchChanges = new LongOpenHashSet();
  private long lookClock;

  public WireSpans spans() {
    return spans;
  }

  public boolean addSpan(WireSpan span) {
    if (!spans.add(span)) {
      return false;
    }
    add(span.fromAnchor());
    add(span.toAnchor());
    connect(span.fromAnchor(), span.toAnchor());
    return true;
  }

  public boolean removeSpan(WireSpan span) {
    WireNetwork network = cells.get(span.fromAnchor());
    if (!spans.remove(span)) {
      return false;
    }
    for (WireCell anchor : List.of(span.fromAnchor(), span.toAnchor())) {
      if (!spans.isAnchor(anchor)) {
        remove(anchor);
      }
    }
    resplit(network);
    return true;
  }

  public void setColumn(BlockPos top, LongList blocks) {
    Set<WireNetwork> before = new HashSet<>();
    for (WireSpan span : spans.at(top)) {
      before.add(cells.get(WireSpan.anchor(top, span.color(), span.owner())));
    }
    spans.setColumn(top, blocks);
    before.forEach(this::resplit);
    for (WireSpan span : spans.at(top)) {
      WireCell anchor = WireSpan.anchor(top, span.color(), span.owner());
      for (WireCell neighbour : adjacentCells(anchor)) {
        connect(anchor, neighbour);
      }
    }
  }

  private void resplit(WireNetwork network) {
    if (network != null && networks.contains(network) && !network.cells.isEmpty()) {
      splitIfNeeded(network, List.copyOf(network.cells));
    }
  }

  private void connect(WireCell first, WireCell second) {
    WireNetwork a = cells.get(first);
    WireNetwork b = cells.get(second);
    if (a == null || b == null || a == b) {
      return;
    }
    WireNetwork target = a.size() >= b.size() ? a : b;
    absorb(target, target == a ? b : a);
    wireChanged(target);
  }

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

  public boolean isLive(long pos) {
    return isOn(pos) && hasPower(pos);
  }

  public boolean isOn(long pos) {
    for (WireNetwork network : networksTouching(pos)) {
      if (network.isOn()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasPower(long pos) {
    for (WireNetwork network : networksTouching(pos)) {
      if (network.gridState().givesPower()) {
        return true;
      }
    }
    return false;
  }

  public long nextLookStamp() {
    return ++lookClock;
  }

  public long lookClock() {
    return lookClock;
  }

  void restoreLookClock(long clock) {
    lookClock = clock;
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

  public long[] drainTouchChanges() {
    long[] drained = touchChanges.toLongArray();
    touchChanges.clear();
    return drained;
  }

  public int sizeAfterAdding(WireCell cell) {
    Set<WireNetwork> joined = adjacentNetworks(cell);
    if (cell.isSealed()) {
      for (WireNetwork open : openNetworksAround(cell)) {
        joined.add(open);
        joined.addAll(networksInLayer(open.cells, cell.owner()));
      }
    }
    return 1 + totalSize(joined);
  }

  public int sizeAfterSpan(WireSpan span) {
    Set<WireNetwork> joined = new HashSet<>();
    for (WireCell anchor : List.of(span.fromAnchor(), span.toAnchor())) {
      if (cells.containsKey(anchor)) {
        joined.add(cells.get(anchor));
      }
      for (WireFace face : facesTouchingColumn(anchor.face().pos())) {
        WireNetwork touching = cells.get(anchor.onFace(face));
        if (touching != null) {
          joined.add(touching);
        }
      }
    }
    return 1 + totalSize(joined);
  }

  public Set<WireNetwork> spanNetworksAt(BlockPos top) {
    Set<WireNetwork> found = new LinkedHashSet<>();
    for (WireSpan span : spans.at(top)) {
      WireNetwork network =
          cells.get(
              span.fromAnchor().face().pos().equals(top) ? span.fromAnchor() : span.toAnchor());
      if (network != null) {
        found.add(network);
      }
    }
    return found;
  }

  public Set<UUID> sealedOwnersTouchingColumn(BlockPos top, Hue color) {
    Set<UUID> owners = new LinkedHashSet<>();
    for (WireFace face : facesTouchingColumn(top)) {
      for (WireCell cell : cellsOn(face.pos())) {
        if (cell.face().equals(face) && cell.color() == color && cell.isSealed()) {
          owners.add(cell.owner());
        }
      }
    }
    return owners;
  }

  public int sizeAfterJoining(WireCell cell) {
    return 1 + totalSize(adjacentNetworks(cell));
  }

  public int sizeAfterReowning(WireNetwork network, UUID owner) {
    Set<WireNetwork> joined = networksInLayer(network.cells, owner);
    joined.remove(network);
    return network.size() + totalSize(joined);
  }

  public Set<WireNetwork> openNetworksAround(WireCell cell) {
    return networksInLayer(List.of(cell), null);
  }

  public Set<WireNetwork> networksNear(ChunkPos center, int radius) {
    Set<WireNetwork> near = new HashSet<>();
    for (int x = center.x - radius; x <= center.x + radius; x++) {
      for (int z = center.z - radius; z <= center.z + radius; z++) {
        for (WireCell cell : cellsInChunk(ChunkPos.toLong(x, z))) {
          near.add(cells.get(cell));
        }
      }
    }
    return near;
  }

  public Set<UUID> sealedOwnersAround(WireFace face, Hue color) {
    List<WireFace> around = new ArrayList<>(WireTopology.neighbours(face));
    around.add(face);
    Set<UUID> owners = new HashSet<>();
    for (WireFace near : around) {
      for (WireCell cell : cellsOn(near.pos())) {
        if (cell.isSealed() && cell.color() == color && cell.face().equals(near)) {
          owners.add(cell.owner());
        }
      }
    }
    return owners;
  }

  public List<WireCell> reown(WireNetwork network, UUID owner) {
    spans.reown(network.cells.stream().filter(spans::isAnchor).toList(), owner);
    List<WireCell> rekeyed = new ArrayList<>(network.size());
    for (WireCell cell : List.copyOf(network.cells)) {
      unplace(cell, network);
      rekeyed.add(cell.ownedBy(owner));
    }
    network.setOwner(owner);
    List<WireCell> doubled = new ArrayList<>();
    Set<WireNetwork> merged = new HashSet<>();
    for (WireCell cell : rekeyed) {
      WireNetwork already = cells.get(cell);
      if (already == null) {
        place(cell, network);
      } else {
        doubled.add(cell);
        merged.add(already);
      }
    }
    merged.addAll(networksInLayer(rekeyed, owner));
    merged.remove(network);
    merged.forEach(other -> absorb(network, other));
    List.copyOf(network.cells).forEach(this::refreshTouches);
    if (merged.isEmpty()) {
      dirty.add(network);
      markChanged(network);
    } else {
      wireChanged(network);
    }
    return doubled;
  }

  public WireNetwork add(WireCell cell) {
    WireNetwork existing = cells.get(cell);
    if (existing != null) {
      return existing;
    }
    Set<WireNetwork> adjacent = adjacentNetworks(cell);
    WireNetwork target =
        adjacent.isEmpty() ? newNetwork(cell.color(), cell.owner()) : largest(adjacent);
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
    unplace(cell, network);
    chunkVersions.addTo(chunkKey(cell), 1);
    adjacent.forEach(this::refreshTouches);
    if (network.cells.isEmpty()) {
      networks.remove(network);
      dirty.remove(network);
      return;
    }
    splitIfNeeded(network, adjacent.isEmpty() ? List.copyOf(network.cells) : adjacent);
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
    WireNetwork fresh = newNetwork(network.color(), network.owner());
    fresh.setOn(network.isOn());
    fresh.setLook(network.look());
    fresh.stampLook(network.lookStamp());
    fresh.rememberedInputs().addAll(network.rememberedInputs());
    fresh.inheritPower(network);
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
      target.stampLook(other.lookStamp());
    }
    target.rememberedInputs().addAll(other.rememberedInputs());
    target.inheritPower(other);
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

  private void unplace(WireCell cell, WireNetwork network) {
    clearTouches(cell, network);
    cells.remove(cell);
    removeFromIndex(byHost, hostKey(cell), cell);
    removeFromIndex(byChunk, chunkKey(cell), cell);
    leave(cell, network);
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
        spans.isAnchor(cell)
            ? List.of()
            : WireTopology.touches(
                cell.face(),
                WireTopology.arms(cell.face(), face -> cells.containsKey(cell.onFace(face))));
    cellTouches.put(cell, touches);
    for (TouchPoint touch : touches) {
      network.touches.addTo(touch, 1);
      long key = touch.pos().asLong();
      touchChanges.add(key);
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
      touchChanges.add(key);
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

  private Set<WireNetwork> networksInLayer(Collection<WireCell> around, UUID owner) {
    Set<WireNetwork> found = new HashSet<>();
    for (WireCell cell : around) {
      WireCell inLayer = cell.ownedBy(owner);
      if (cells.containsKey(inLayer)) {
        found.add(cells.get(inLayer));
      }
      found.addAll(adjacentNetworks(inLayer));
    }
    return found;
  }

  private List<WireCell> adjacentCells(WireCell cell) {
    Set<WireCell> adjacent = new LinkedHashSet<>();
    for (WireFace face : WireTopology.neighbours(cell.face())) {
      adjacent.add(cell.onFace(face));
    }
    adjacent.addAll(spans.linkedTo(cell));
    if (spans.isAnchor(cell)) {
      facesTouchingColumn(cell.face().pos()).forEach(face -> adjacent.add(cell.onFace(face)));
    } else {
      for (long block : new long[] {hostKey(cell), cell.face().occupant().asLong()}) {
        WireCell anchor = spans.anchorFor(block, cell);
        if (anchor != null) {
          adjacent.add(anchor);
        }
      }
    }
    adjacent.remove(cell);
    adjacent.removeIf(neighbour -> !cells.containsKey(neighbour));
    return new ArrayList<>(adjacent);
  }

  private List<WireFace> facesTouchingColumn(BlockPos top) {
    LongList column = spans.column(top);
    if (column == null) {
      return List.of();
    }
    List<WireFace> faces = new ArrayList<>();
    column.forEach(
        (long block) -> {
          BlockPos pos = BlockPos.fromLong(block);
          for (Direction side : Direction.values()) {
            faces.add(new WireFace(pos, side));
            faces.add(new WireFace(pos.offset(side), side.getOpposite()));
          }
        });
    return faces;
  }

  private WireNetwork newNetwork(Hue color, UUID owner) {
    WireNetwork network = new WireNetwork(color, owner);
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

  private static int totalSize(Set<WireNetwork> networks) {
    int size = 0;
    for (WireNetwork network : networks) {
      size += network.size();
    }
    return size;
  }

  private static long hostKey(WireCell cell) {
    return cell.face().pos().asLong();
  }

  private static long chunkKey(WireCell cell) {
    return ChunkPos.toLong(cell.face().pos());
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
