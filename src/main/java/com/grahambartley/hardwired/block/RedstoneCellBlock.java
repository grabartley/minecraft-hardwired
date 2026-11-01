package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.power.SourceKind;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public final class RedstoneCellBlock extends FuelledSourceBlock {

  private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 14, 14);

  public RedstoneCellBlock(Settings settings) {
    super(settings);
  }

  @Override
  public SourceKind sourceKind() {
    return SourceKind.CELL;
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new RedstoneCellBlockEntity(pos, state);
  }
}
