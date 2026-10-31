package com.grahambartley.hardwired.wire;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

public final class WireState extends PersistentState {

  public static final String KEY = "hardwired_wires";
  public static final Type<WireState> TYPE =
      new Type<>(
          () -> new WireState(new WireGrid()),
          (nbt, registries) -> new WireState(WireGridNbt.read(nbt)),
          null);

  private final WireGrid grid;

  private WireState(WireGrid grid) {
    this.grid = grid;
  }

  public WireGrid grid() {
    return grid;
  }

  @Override
  public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    return nbt.copyFrom(WireGridNbt.write(grid));
  }
}
