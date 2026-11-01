package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.power.SourceKind;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public final class RedstoneGeneratorBlock extends FuelledSourceBlock {

  public RedstoneGeneratorBlock(Settings settings) {
    super(settings);
  }

  @Override
  public SourceKind sourceKind() {
    return SourceKind.GENERATOR;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new RedstoneGeneratorBlockEntity(pos, state);
  }
}
