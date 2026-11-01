package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.power.GridState;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireGridNbt {

  private static final String NETWORKS = "networks";
  private static final String OWNER = "owner";
  private static final String COLOR = "color";
  private static final String ON = "on";
  private static final String TRIPPED = "tripped";
  private static final String LOOK_COLOR = "look_color";
  private static final String LOOK_BRIGHTNESS = "look_brightness";
  private static final String LOOK_STAMP = "look_stamp";
  private static final String LOOK_CLOCK = "look_clock";
  private static final String REMEMBERED = "remembered";
  private static final String POSITIONS = "positions";
  private static final String SIDES = "sides";
  private static final String GRID_STATE = "grid_state";
  private static final String DARK_UNTIL = "dark_until";
  private static final String LAST_LOADS = "last_loads";
  private static final String LAST_RATINGS = "last_ratings";
  private static final String LAST_SOURCES = "last_sources";
  private static final String LINKS = "links";
  private static final String MASTERS = "masters";
  private static final String VALUES = "_values";

  private WireGridNbt() {}

  public static NbtCompound write(WireGrid grid) {
    NbtList networks = new NbtList();
    for (WireNetwork network : grid.networks()) {
      networks.add(writeNetwork(network));
    }
    NbtCompound root = new NbtCompound();
    root.put(NETWORKS, networks);
    root.putLong(LOOK_CLOCK, grid.lookClock());
    return root;
  }

  public static WireGrid read(NbtCompound root) {
    WireGrid grid = new WireGrid();
    NbtList networks = root.getList(NETWORKS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < networks.size(); i++) {
      readNetwork(grid, networks.getCompound(i));
    }
    grid.restoreLookClock(root.getLong(LOOK_CLOCK));
    grid.drainDirty();
    return grid;
  }

  private static NbtCompound writeNetwork(WireNetwork network) {
    List<WireCell> cells = List.copyOf(network.cells());
    long[] positions = new long[cells.size()];
    byte[] sides = new byte[cells.size()];
    for (int i = 0; i < cells.size(); i++) {
      positions[i] = cells.get(i).face().pos().asLong();
      sides[i] = (byte) cells.get(i).face().side().getId();
    }
    NbtCompound nbt = new NbtCompound();
    nbt.putString(COLOR, network.color().asString());
    nbt.putBoolean(ON, network.isOn());
    nbt.putBoolean(TRIPPED, network.isTripped());
    nbt.putLongArray(POSITIONS, positions);
    nbt.putByteArray(SIDES, sides);
    if (network.isSealed()) {
      nbt.putUuid(OWNER, network.owner());
    }
    nbt.putLongArray(REMEMBERED, network.rememberedInputs().toLongArray());
    nbt.putByte(GRID_STATE, (byte) network.gridState().ordinal());
    nbt.putLong(DARK_UNTIL, network.darkUntil());
    writeFacts(nbt, LAST_LOADS, network.lastLoads());
    writeFacts(nbt, LAST_RATINGS, network.lastRatings());
    writeFacts(nbt, LAST_SOURCES, network.lastSources());
    nbt.putLongArray(LINKS, network.packedLinks());
    nbt.putLongArray(MASTERS, network.masters().toLongArray());
    if (network.look() != null) {
      nbt.putString(LOOK_COLOR, network.look().color().asString());
      nbt.putByte(LOOK_BRIGHTNESS, (byte) network.look().brightness());
      nbt.putLong(LOOK_STAMP, network.lookStamp());
    }
    return nbt;
  }

  private static void writeFacts(NbtCompound nbt, String key, Long2IntOpenHashMap facts) {
    long[] positions = facts.keySet().toLongArray();
    int[] values = new int[positions.length];
    for (int i = 0; i < positions.length; i++) {
      values[i] = facts.get(positions[i]);
    }
    nbt.putLongArray(key, positions);
    nbt.putIntArray(key + VALUES, values);
  }

  private static void readFacts(NbtCompound nbt, String key, Long2IntOpenHashMap facts) {
    long[] positions = nbt.getLongArray(key);
    int[] values = nbt.getIntArray(key + VALUES);
    for (int i = 0; i < Math.min(positions.length, values.length); i++) {
      facts.put(positions[i], values[i]);
    }
  }

  private static void readNetwork(WireGrid grid, NbtCompound nbt) {
    Hue color = Hue.byName(nbt.getString(COLOR));
    long[] positions = nbt.getLongArray(POSITIONS);
    byte[] sides = nbt.getByteArray(SIDES);
    UUID owner = nbt.containsUuid(OWNER) ? nbt.getUuid(OWNER) : null;
    WireNetwork network = null;
    for (int i = 0; i < Math.min(positions.length, sides.length); i++) {
      network =
          grid.add(
              new WireCell(
                  new WireFace(BlockPos.fromLong(positions[i]), Direction.byId(sides[i])),
                  color,
                  owner));
    }
    if (network == null) {
      return;
    }
    network.setOn(nbt.getBoolean(ON));
    network.setTripped(nbt.getBoolean(TRIPPED));
    for (long pos : nbt.getLongArray(REMEMBERED)) {
      network.rememberedInputs().add(pos);
    }
    network.setGridState(GridState.byIndex(nbt.getByte(GRID_STATE)));
    network.setDarkUntil(nbt.getLong(DARK_UNTIL));
    readFacts(nbt, LAST_LOADS, network.lastLoads());
    readFacts(nbt, LAST_RATINGS, network.lastRatings());
    readFacts(nbt, LAST_SOURCES, network.lastSources());
    network.unpackLinks(nbt.getLongArray(LINKS));
    for (long pos : nbt.getLongArray(MASTERS)) {
      network.masters().add(pos);
    }
    if (nbt.contains(LOOK_COLOR)) {
      network.setLook(
          new LampLook(Hue.byName(nbt.getString(LOOK_COLOR)), nbt.getByte(LOOK_BRIGHTNESS)));
      network.stampLook(nbt.getLong(LOOK_STAMP));
    }
  }
}
