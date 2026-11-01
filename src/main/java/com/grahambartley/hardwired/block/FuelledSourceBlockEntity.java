package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.power.CellRules;
import com.grahambartley.hardwired.power.ChargeStore;
import com.grahambartley.hardwired.power.PowerWear;
import com.grahambartley.hardwired.registry.ModComponents;
import java.util.function.ToIntFunction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public abstract class FuelledSourceBlockEntity extends SourceBlockEntity implements ChargeStore {

  private static final String CHARGE = "charge";
  private static final String PART = "charge_part";

  private final ToIntFunction<PowerSettings> capacity;
  private int charge;
  private double part;

  protected FuelledSourceBlockEntity(
      BlockEntityType<?> type,
      BlockPos pos,
      BlockState state,
      ToIntFunction<PowerSettings> capacity) {
    super(type, pos, state);
    this.capacity = capacity;
  }

  @Override
  public int charge() {
    return charge;
  }

  @Override
  public int capacity() {
    return capacity.applyAsInt(Hardwired.SERVER_CONFIG.power());
  }

  @Override
  public void setCharge(int next) {
    charge = Math.max(next, 0);
    markDirty();
    showLevel();
  }

  @Override
  public void burn(double amount) {
    part -= amount;
    int whole = PowerWear.whole(-part);
    if (whole > 0) {
      drain(whole);
      part += whole;
    }
    markDirty();
  }

  @Override
  public void earn(double amount) {
    part += amount;
    int whole = PowerWear.whole(part);
    if (whole > 0) {
      fill(whole);
      part -= whole;
      markDirty();
    }
  }

  public void showLevel() {
    int level = CellRules.level(charge, capacity());
    BlockState state = getCachedState();
    if (world != null && !world.isClient && state.get(FuelledSourceBlock.LEVEL) != level) {
      world.setBlockState(pos, state.with(FuelledSourceBlock.LEVEL, level), Block.NOTIFY_ALL);
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putInt(CHARGE, charge);
    nbt.putDouble(PART, part);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    charge = Math.max(nbt.getInt(CHARGE), 0);
    part = nbt.getDouble(PART);
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
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(CHARGE);
    nbt.remove(PART);
  }
}
