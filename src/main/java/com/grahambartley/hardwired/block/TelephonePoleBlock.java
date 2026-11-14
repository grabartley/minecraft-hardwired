package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.line.PoleLines;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public final class TelephonePoleBlock extends Block {

  public static final EnumProperty<PolePart> PART = EnumProperty.of("part", PolePart.class);
  public static final double ARM_HEIGHT = 12.5 / 16.0;
  public static final double ARM_LENGTH = 14.0 / 16.0;
  private static final VoxelShape POST = Block.createCuboidShape(5, 0, 5, 11, 16, 11);
  private static final VoxelShape ARM_X =
      VoxelShapes.union(POST, Block.createCuboidShape(1, 11, 6, 15, 14, 10));
  private static final VoxelShape ARM_Z =
      VoxelShapes.union(POST, Block.createCuboidShape(6, 11, 1, 10, 14, 15));

  public TelephonePoleBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(PART, PolePart.SINGLE)
            .with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
  }

  public static boolean isPole(BlockState state) {
    return state.getBlock() instanceof TelephonePoleBlock;
  }

  public static boolean isTop(BlockView world, BlockPos pos) {
    return isPole(world.getBlockState(pos)) && !isPole(world.getBlockState(pos.up()));
  }

  private static PolePart partAt(BlockView world, BlockPos pos) {
    return PolePart.between(
        isPole(world.getBlockState(pos.down())), isPole(world.getBlockState(pos.up())));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(PART, Properties.HORIZONTAL_AXIS);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockState below = context.getWorld().getBlockState(context.getBlockPos().down());
    Direction.Axis axis =
        isPole(below)
            ? below.get(Properties.HORIZONTAL_AXIS)
            : context.getHorizontalPlayerFacing().rotateYClockwise().getAxis();
    return getDefaultState()
        .with(PART, partAt(context.getWorld(), context.getBlockPos()))
        .with(Properties.HORIZONTAL_AXIS, axis);
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighbor,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    return direction.getAxis() == Direction.Axis.Y ? state.with(PART, partAt(world, pos)) : state;
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    if (!state.get(PART).holdsSpans()) {
      return POST;
    }
    return state.get(Properties.HORIZONTAL_AXIS) == Direction.Axis.X ? ARM_X : ARM_Z;
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this) && world instanceof ServerWorld server) {
      PoleLines.of(server).columnChanged(pos);
    }
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock()) && world instanceof ServerWorld server) {
      PoleLines.of(server).poleBroken(pos);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }
}
