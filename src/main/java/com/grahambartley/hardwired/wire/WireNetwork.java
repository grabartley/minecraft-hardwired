package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.power.GridState;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongPredicate;
import net.minecraft.util.math.BlockPos;

public final class WireNetwork {

  private final Hue color;
  final Set<WireCell> cells = new HashSet<>();
  final Object2IntOpenHashMap<TouchPoint> touches = new Object2IntOpenHashMap<>();
  final Long2IntOpenHashMap touchedPositions = new Long2IntOpenHashMap();
  final Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
  private final LongOpenHashSet rememberedInputs = new LongOpenHashSet();
  private final TripDetector tripDetector = new TripDetector();
  private final Long2IntOpenHashMap lastLoads = new Long2IntOpenHashMap();
  private final Long2IntOpenHashMap lastRatings = new Long2IntOpenHashMap();
  private final Long2IntOpenHashMap lastSources = new Long2IntOpenHashMap();
  private final Long2ObjectOpenHashMap<long[]> links = new Long2ObjectOpenHashMap<>();
  private final LongOpenHashSet masters = new LongOpenHashSet();
  private GridState gridState = GridState.NO_SOURCE;
  private long darkUntil;
  private int gridLoad;
  private int gridSupply;
  private boolean on;
  private boolean tripped;
  private LampLook look;
  private long lookStamp;
  private UUID owner;

  WireNetwork(Hue color, UUID owner) {
    this.color = color;
    this.owner = owner;
  }

  public Hue color() {
    return color;
  }

  public UUID owner() {
    return owner;
  }

  public boolean isSealed() {
    return owner != null;
  }

  void setOwner(UUID owner) {
    this.owner = owner;
  }

  public int size() {
    return cells.size();
  }

  public Set<WireCell> cells() {
    return cells;
  }

  public LongSet touchedPositions() {
    return touchedPositions.keySet();
  }

  public Set<TouchPoint> touches() {
    return touches.keySet();
  }

  public LongSet chunks() {
    return chunks.keySet();
  }

  public boolean isOn() {
    return on;
  }

  public void setOn(boolean on) {
    this.on = on;
  }

  public Long2IntOpenHashMap lastLoads() {
    return lastLoads;
  }

  public Long2IntOpenHashMap lastRatings() {
    return lastRatings;
  }

  public Long2IntOpenHashMap lastSources() {
    return lastSources;
  }

  public Long2ObjectOpenHashMap<long[]> links() {
    return links;
  }

  public void link(long touched, List<BlockPos> cells) {
    links.put(touched, cells.stream().mapToLong(BlockPos::asLong).toArray());
  }

  public void keepFacts(LongPredicate keepsLink) {
    links.keySet().removeIf((long touched) -> !keepsLink.test(touched));
    LongOpenHashSet kept = linkedCells();
    lastLoads.keySet().retainAll(kept);
    lastRatings.keySet().retainAll(kept);
    lastSources.keySet().retainAll(kept);
    masters.retainAll(kept);
  }

  public LongOpenHashSet linkedCells() {
    LongOpenHashSet cells = new LongOpenHashSet();
    links.values().forEach(each -> cells.addAll(LongList.of(each)));
    return cells;
  }

  public long[] packedLinks() {
    LongArrayList packed = new LongArrayList();
    links.forEach(
        (touched, cells) -> {
          packed.add((long) touched);
          packed.add(cells.length);
          packed.addElements(packed.size(), cells);
        });
    return packed.toLongArray();
  }

  public void unpackLinks(long[] packed) {
    int at = 0;
    while (at + 1 < packed.length) {
      int count = (int) packed[at + 1];
      links.put(packed[at], Arrays.copyOfRange(packed, at + 2, at + 2 + count));
      at += 2 + count;
    }
  }

  public LongOpenHashSet masters() {
    return masters;
  }

  public GridState gridState() {
    return gridState;
  }

  public void setGridState(GridState gridState) {
    this.gridState = gridState;
  }

  public long darkUntil() {
    return darkUntil;
  }

  public void setDarkUntil(long darkUntil) {
    this.darkUntil = darkUntil;
  }

  public int gridLoad() {
    return gridLoad;
  }

  public int gridSupply() {
    return gridSupply;
  }

  public void showTotals(int load, int supply) {
    gridLoad = load;
    gridSupply = supply;
  }

  void inheritPower(WireNetwork other) {
    lastLoads.putAll(other.lastLoads);
    lastRatings.putAll(other.lastRatings);
    lastSources.putAll(other.lastSources);
    links.putAll(other.links);
    masters.addAll(other.masters);
    gridState = gridState.higherOf(other.gridState);
    darkUntil = Math.max(darkUntil, other.darkUntil);
  }

  public boolean sharesPowerWith(WireNetwork other) {
    return Objects.equals(owner, other.owner);
  }

  public boolean isTripped() {
    return tripped;
  }

  public void setTripped(boolean tripped) {
    this.tripped = tripped;
  }

  public TripDetector tripDetector() {
    return tripDetector;
  }

  public LampLook look() {
    return look;
  }

  public void setLook(LampLook look) {
    this.look = look;
  }

  public long lookStamp() {
    return lookStamp;
  }

  public void stampLook(long stamp) {
    lookStamp = stamp;
  }

  public boolean isTouching(BlockPos pos) {
    return touchedPositions.containsKey(pos.asLong());
  }

  public LongSet rememberedInputs() {
    return rememberedInputs;
  }
}
