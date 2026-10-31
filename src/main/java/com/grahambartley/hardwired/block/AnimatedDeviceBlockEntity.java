package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public final class AnimatedDeviceBlockEntity extends BlockEntity {

  public AnimatedDeviceBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.ANIMATED_DEVICE, pos, state);
  }
}
