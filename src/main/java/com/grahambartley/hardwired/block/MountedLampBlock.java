package com.grahambartley.hardwired.block;

import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class MountedLampBlock extends AbstractLampBlock {

  public static final EnumProperty<BlockFace> FACE = Properties.BLOCK_FACE;
  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

  private final double[] floorBox;
  private final Set<BlockFace> faces;

  public MountedLampBlock(Settings settings, double[] floorBox, Set<BlockFace> faces) {
    super(settings);
    this.floorBox = floorBox;
    this.faces = faces;
    setDefaultState(
        getDefaultState().with(FACE, faces.iterator().next()).with(FACING, Direction.NORTH));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(FACE, FACING);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return MountShapes.shape(floorBox, state.get(FACE), state.get(FACING));
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    for (Direction direction : context.getPlacementDirections()) {
      BlockFace face = faceFor(direction);
      if (!faces.contains(face)) {
        continue;
      }
      Direction facing =
          direction.getAxis() == Direction.Axis.Y
              ? context.getHorizontalPlayerFacing()
              : direction.getOpposite();
      BlockState state = getDefaultState().with(FACE, face).with(FACING, facing);
      if (canPlaceAt(state, context.getWorld(), context.getBlockPos())) {
        return state;
      }
    }
    return null;
  }

  @Override
  protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    Direction attached = attachedDirection(state);
    return WallMountedBlock.canPlaceAt(world, pos, attached);
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    return direction == attachedDirection(state) && !canPlaceAt(state, world, pos)
        ? Blocks.AIR.getDefaultState()
        : state;
  }

  private static BlockFace faceFor(Direction direction) {
    return switch (direction) {
      case UP -> BlockFace.CEILING;
      case DOWN -> BlockFace.FLOOR;
      default -> BlockFace.WALL;
    };
  }

  private static Direction attachedDirection(BlockState state) {
    return switch (state.get(FACE)) {
      case CEILING -> Direction.UP;
      case FLOOR -> Direction.DOWN;
      case WALL -> state.get(FACING).getOpposite();
    };
  }
}
