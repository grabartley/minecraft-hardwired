package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.power.CellRules;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class RedstoneGeneratorBlockEntity extends FuelledSourceBlockEntity
    implements SidedInventory {

  private static final int[] FUEL_SLOT = {0};

  public RedstoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.REDSTONE_GENERATOR, pos, state, PowerSettings::generatorCapacity);
  }

  private int worth(ItemStack stack) {
    int dust = Hardwired.SERVER_CONFIG.power().dustCharge();
    if (stack.isOf(Items.REDSTONE)) {
      return CellRules.worth(dust, false);
    }
    return stack.isOf(Items.REDSTONE_BLOCK) ? CellRules.worth(dust, true) : 0;
  }

  @Override
  public int[] getAvailableSlots(Direction side) {
    return FUEL_SLOT;
  }

  @Override
  public boolean canInsert(int slot, ItemStack stack, Direction side) {
    return CellRules.takes(charge(), capacity(), worth(stack));
  }

  @Override
  public boolean isValid(int slot, ItemStack stack) {
    return canInsert(slot, stack, null);
  }

  @Override
  public boolean canExtract(int slot, ItemStack stack, Direction side) {
    return false;
  }

  @Override
  public int getMaxCountPerStack() {
    return 1;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public ItemStack getStack(int slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public ItemStack removeStack(int slot, int amount) {
    return ItemStack.EMPTY;
  }

  @Override
  public ItemStack removeStack(int slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public void setStack(int slot, ItemStack stack) {
    fill(worth(stack) * stack.getCount());
  }

  @Override
  public boolean canPlayerUse(PlayerEntity player) {
    return false;
  }

  @Override
  public void clear() {}
}
