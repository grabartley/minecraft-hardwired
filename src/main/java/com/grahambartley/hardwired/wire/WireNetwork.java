package com.grahambartley.hardwired.wire;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashSet;
import java.util.Set;

public final class WireNetwork {

  private final Hue color;
  final Set<WireCell> cells = new HashSet<>();
  final Object2IntOpenHashMap<TouchPoint> touches = new Object2IntOpenHashMap<>();
  final Long2IntOpenHashMap touchedPositions = new Long2IntOpenHashMap();
  final Long2IntOpenHashMap chunks = new Long2IntOpenHashMap();
  private final LongOpenHashSet rememberedInputs = new LongOpenHashSet();
  private final TripDetector tripDetector = new TripDetector();
  private boolean on;
  private boolean tripped;
  private LampLook look;

  WireNetwork(Hue color) {
    this.color = color;
  }

  public Hue color() {
    return color;
  }

  public int size() {
    return cells.size();
  }

  public Set<WireCell> cells() {
    return cells;
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

  public LongSet rememberedInputs() {
    return rememberedInputs;
  }
}
