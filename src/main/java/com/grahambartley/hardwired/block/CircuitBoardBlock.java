package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DeviceActions;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class CircuitBoardBlock extends HorizontalFacingBlock implements BlockEntityProvider {

  private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 12, 16);

  public CircuitBoardBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new CircuitBoardBlockEntity(pos, state);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return DeviceActions.open(player, pos);
  }
}
