package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public final class RedstoneCellBlockEntity extends FuelledSourceBlockEntity {

  public RedstoneCellBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.REDSTONE_CELL, pos, state, PowerSettings::cellCapacity);
  }

  @Override
  public boolean acceptsEarnings() {
    return true;
  }
}
