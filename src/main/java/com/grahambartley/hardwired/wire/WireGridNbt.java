package com.grahambartley.hardwired.wire;

import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireGridNbt {

  private static final String NETWORKS = "networks";
  private static final String COLOR = "color";
  private static final String ON = "on";
  private static final String TRIPPED = "tripped";
  private static final String LOOK_COLOR = "look_color";
  private static final String LOOK_BRIGHTNESS = "look_brightness";
  private static final String REMEMBERED = "remembered";
  private static final String POSITIONS = "positions";
  private static final String SIDES = "sides";

  private WireGridNbt() {}

  public static NbtCompound write(WireGrid grid) {
    NbtList networks = new NbtList();
    for (WireNetwork network : grid.networks()) {
      networks.add(writeNetwork(network));
    }
    NbtCompound root = new NbtCompound();
    root.put(NETWORKS, networks);
    return root;
  }

  public static WireGrid read(NbtCompound root) {
    WireGrid grid = new WireGrid();
    NbtList networks = root.getList(NETWORKS, NbtElement.COMPOUND_TYPE);
    for (int i = 0; i < networks.size(); i++) {
      readNetwork(grid, networks.getCompound(i));
    }
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
    nbt.putLongArray(REMEMBERED, network.rememberedInputs().toLongArray());
    if (network.look() != null) {
      nbt.putString(LOOK_COLOR, network.look().color().asString());
      nbt.putByte(LOOK_BRIGHTNESS, (byte) network.look().brightness());
    }
    return nbt;
  }

  private static void readNetwork(WireGrid grid, NbtCompound nbt) {
    Hue color = Hue.byName(nbt.getString(COLOR));
    long[] positions = nbt.getLongArray(POSITIONS);
    byte[] sides = nbt.getByteArray(SIDES);
    WireNetwork network = null;
    for (int i = 0; i < Math.min(positions.length, sides.length); i++) {
      network =
          grid.add(
              new WireCell(
                  new WireFace(BlockPos.fromLong(positions[i]), Direction.byId(sides[i])), color));
    }
    if (network == null) {
      return;
    }
    network.setOn(nbt.getBoolean(ON));
    network.setTripped(nbt.getBoolean(TRIPPED));
    for (long pos : nbt.getLongArray(REMEMBERED)) {
      network.rememberedInputs().add(pos);
    }
    if (nbt.contains(LOOK_COLOR)) {
      network.setLook(
          new LampLook(Hue.byName(nbt.getString(LOOK_COLOR)), nbt.getByte(LOOK_BRIGHTNESS)));
    }
  }
}
