package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCorner;
import com.grahambartley.hardwired.registry.ModEntities;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class LargeLiftBlock extends LiftBlock {

  public static final EnumProperty<LiftCorner> CORNER = EnumProperty.of("corner", LiftCorner.class);

  private static final int SPAN = 2;

  public LargeLiftBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(CORNER, LiftCorner.NORTH_WEST));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(CORNER);
  }

  @Override
  public int span() {
    return SPAN;
  }

  @Override
  public BlockPos head(BlockPos pos, BlockState state) {
    return state.get(CORNER).head(pos);
  }

  @Override
  public EntityType<LiftCarEntity> carType() {
    return ModEntities.LARGE_LIFT_CAR;
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    return LiftCorner.cells(head(pos, state));
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return SPAN * super.ratedLoad(settings);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    Direction look = context.getHorizontalPlayerFacing();
    BlockPos pos = context.getBlockPos();
    BlockPos head = LiftCorner.headFor(pos, look);
    BlockState placed = cellState(LiftCorner.of(head, pos), look.getOpposite());
    World world = context.getWorld();
    for (BlockPos cell : LiftCorner.cells(head)) {
      if (!world.getWorldBorder().contains(cell)
          || !world.getBlockState(cell).canReplace(context)
          || !world.canPlace(placed, cell, ShapeContext.absent())) {
        return null;
      }
    }
    return placed;
  }

  private BlockState cellState(LiftCorner corner, Direction front) {
    return getDefaultState().with(CORNER, corner).with(FACING, corner.facing(front));
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    Direction front =
        placer == null ? state.get(FACING) : placer.getHorizontalFacing().getOpposite();
    BlockPos head = head(pos, state);
    for (LiftCorner corner : LiftCorner.values()) {
      BlockPos cell = corner.cell(head);
      if (!cell.equals(pos)) {
        world.setBlockState(cell, cellState(corner, front), Block.NOTIFY_ALL);
      }
    }
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
