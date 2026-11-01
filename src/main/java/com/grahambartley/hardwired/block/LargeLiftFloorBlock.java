package com.grahambartley.hardwired.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class LargeLiftFloorBlock extends LiftFloorBlock {

  public static final BooleanProperty SECOND = BooleanProperty.of("second");

  public LargeLiftFloorBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(SECOND, false));
  }

  private static Direction along(Direction facing) {
    return facing.rotateYClockwise();
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(SECOND);
  }

  @Override
  public BlockPos origin(BlockPos pos, BlockState state) {
    BlockPos lower = super.origin(pos, state);
    return state.get(SECOND) ? lower.offset(along(state.get(FACING)).getOpposite()) : lower;
  }

  private static List<BlockPos> cells(BlockPos origin, Direction facing) {
    BlockPos second = origin.offset(along(facing));
    return List.of(origin, origin.up(), second, second.up());
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    return cells(origin(pos, state), state.get(FACING));
  }

  @Override
  public List<BlockPos> columns(BlockPos pos, BlockState state) {
    Direction facing = state.get(FACING);
    BlockPos first = origin(pos, state).offset(facing);
    return List.of(first, first.offset(along(facing)));
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockState placed = getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
    World world = context.getWorld();
    BlockPos second = context.getBlockPos().offset(along(placed.get(FACING)));
    for (BlockPos cell : cells(context.getBlockPos(), placed.get(FACING))) {
      if (!world.isInBuildLimit(cell)
          || !world.getWorldBorder().contains(cell)
          || !world.getBlockState(cell).canReplace(context)
          || !world.canPlace(placed, cell, ShapeContext.absent())) {
        return null;
      }
    }
    return placed.canPlaceAt(world, context.getBlockPos()) && placed.canPlaceAt(world, second)
        ? placed
        : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    BlockPos second = pos.offset(along(state.get(FACING)));
    world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    world.setBlockState(second, state.with(SECOND, true), Block.NOTIFY_ALL);
    world.setBlockState(
        second.up(), state.with(SECOND, true).with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(HALF) == DoubleBlockHalf.UPPER && !state.get(SECOND)
        ? new LiftFloorBlockEntity(pos, state)
        : new ElectronicBlockEntity(pos, state);
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState next, boolean moved) {
    if (!state.isOf(next.getBlock())) {
      for (BlockPos cell : cells(pos, state)) {
        if (!cell.equals(pos) && world.getBlockState(cell).isOf(this)) {
          world.setBlockState(
              cell, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
        }
      }
    }
    super.onStateReplaced(state, world, pos, next, moved);
  }
}
