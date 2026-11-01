package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.power.GridState;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
  private final LongOpenHashSet links = new LongOpenHashSet();
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

  public LongOpenHashSet links() {
    return links;
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

  public boolean isLive() {
    return on && gridState.givesPower();
  }

  void inheritPower(WireNetwork other) {
    lastLoads.putAll(other.lastLoads);
    lastRatings.putAll(other.lastRatings);
    lastSources.putAll(other.lastSources);
    links.addAll(other.links);
    masters.addAll(other.masters);
    gridState = gridState.ordinal() > other.gridState.ordinal() ? gridState : other.gridState;
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
