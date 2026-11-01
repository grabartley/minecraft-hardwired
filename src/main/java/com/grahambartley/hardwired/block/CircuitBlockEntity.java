package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Evaluator;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.device.DesignCodecs;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class CircuitBlockEntity extends BlockEntity {

  private static final String DESIGN = "design";
  private static final String TURNS = "turns";
  private static final String MEMORY = "memory";
  private static final String OUTPUTS = "outputs";

  private Design design = Design.EMPTY;
  private Evaluator evaluator = new Evaluator(design);
  private int turns;

  public CircuitBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.CIRCUIT_BLOCK, pos, state);
  }

  public Design design() {
    return design;
  }

  public int turns() {
    return turns;
  }

  public int[] memory() {
    return evaluator.memory();
  }

  private void setDesign(Design next) {
    design = next;
    evaluator = new Evaluator(next);
  }

  public void setTurns(int quarterTurns) {
    turns = Math.floorMod(quarterTurns, Side.COUNT);
    markDirty();
  }

  public boolean drives(Direction worldSide) {
    Side side = CircuitSides.side(worldSide);
    return side != null && (evaluator.outputs() & side.turned(-turns).bit()) != 0;
  }

  public boolean step(ServerWorld world) {
    if (!WireEngine.of(world).power().hasPower(pos)) {
      return true;
    }
    int outputsBefore = evaluator.outputs();
    evaluator.step(readInputs(world));
    if (!evaluator.isSettled() || evaluator.outputs() != outputsBefore) {
      markDirty();
    }
    if (evaluator.outputs() != outputsBefore) {
      WireEngine.of(world).refresh(pos);
    }
    return evaluator.isSettled();
  }

  private int readInputs(ServerWorld world) {
    WireEngine engine = WireEngine.of(world);
    int inputs = 0;
    for (Side side : Side.values()) {
      if (design.mode(side) == PortMode.INPUT
          && engine.isOnFrom(pos, CircuitSides.direction(side.turned(turns)))) {
        inputs |= side.bit();
      }
    }
    return inputs;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putString(DESIGN, DesignText.encode(design));
    nbt.putInt(TURNS, turns);
    nbt.putIntArray(MEMORY, evaluator.memory());
    nbt.putInt(OUTPUTS, evaluator.outputs());
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    setDesign(DesignCodecs.orEmpty(nbt.getString(DESIGN)));
    turns = Math.floorMod(nbt.getInt(TURNS), Side.COUNT);
    evaluator.restore(nbt.getIntArray(MEMORY), nbt.getInt(OUTPUTS));
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    setDesign(components.getOrDefault(ModComponents.CIRCUIT_DESIGN, Design.EMPTY));
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    if (!design.equals(Design.EMPTY)) {
      builder.add(ModComponents.CIRCUIT_DESIGN, design);
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(DESIGN);
    nbt.remove(TURNS);
    nbt.remove(MEMORY);
    nbt.remove(OUTPUTS);
  }
}
