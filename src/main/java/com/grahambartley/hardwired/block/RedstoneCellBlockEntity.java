package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.power.CellRules;
import com.grahambartley.hardwired.power.ChargeStore;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModComponents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class RedstoneCellBlockEntity extends BlockEntity implements ChargeStore {

  private static final String CHARGE = "charge";

  private int charge;

  public RedstoneCellBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.REDSTONE_CELL, pos, state);
  }

  @Override
  public int charge() {
    return charge;
  }

  @Override
  public int capacity() {
    return Hardwired.SERVER_CONFIG.power().cellCapacity();
  }

  @Override
  public void setCharge(int next) {
    charge = Math.max(next, 0);
    markDirty();
    showLevel();
  }

  public void showLevel() {
    int level = CellRules.level(charge, capacity());
    BlockState state = getCachedState();
    if (world != null && !world.isClient && state.get(RedstoneCellBlock.LEVEL) != level) {
      world.setBlockState(pos, state.with(RedstoneCellBlock.LEVEL, level), Block.NOTIFY_ALL);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putInt(CHARGE, charge);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    charge = Math.max(nbt.getInt(CHARGE), 0);
  }

  @Override
  protected void readComponents(ComponentsAccess components) {
    super.readComponents(components);
    charge = Math.clamp(components.getOrDefault(ModComponents.CELL_CHARGE, 0), 0, capacity());
  }

  @Override
  protected void addComponents(ComponentMap.Builder builder) {
    super.addComponents(builder);
    if (charge > 0) {
      builder.add(ModComponents.CELL_CHARGE, charge);
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(CHARGE);
  }
}
