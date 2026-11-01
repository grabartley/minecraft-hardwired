package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.lift.LiftActions;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;

public final class LiftFloorBlock extends Block implements BlockEntityProvider, WireDevice {

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
  public static final BooleanProperty OPEN = Properties.OPEN;

  private static final Map<Direction, VoxelShape> CLOSED = new EnumMap<>(Direction.class);
  private static final Map<Direction, VoxelShape> OPENED = new EnumMap<>(Direction.class);

  static {
    for (Direction side : Direction.Type.HORIZONTAL) {
      CLOSED.put(side, LiftFloorShapes.closed(side));
      OPENED.put(side, LiftFloorShapes.open(side));
    }
  }

  public LiftFloorBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(HALF, DoubleBlockHalf.LOWER)
            .with(OPEN, false));
  }

  public static BlockPos lower(BlockPos pos, BlockState state) {
    return state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    BlockPos lower = lower(pos, state);
    return List.of(lower, lower.up());
  }

  public static BlockPos column(BlockPos pos, BlockState state) {
    return pos.offset(state.get(FACING));
  }

  @Override
  protected MapCodec<? extends Block> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, HALF, OPEN);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockPos pos = context.getBlockPos();
    World world = context.getWorld();
    if (pos.getY() >= world.getTopY() - 1 || !world.getBlockState(pos.up()).canReplace(context)) {
      return null;
    }
    return getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
  }

  @Override
  protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    BlockPos below = pos.down();
    BlockState under = world.getBlockState(below);
    return state.get(HALF) == DoubleBlockHalf.LOWER
        ? under.isSideSolidFullSquare(world, below, Direction.UP)
        : under.isOf(this);
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighbor,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    DoubleBlockHalf half = state.get(HALF);
    if (direction.getAxis() == Direction.Axis.Y
        && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
      return neighbor.isOf(this) && neighbor.get(HALF) != half
          ? neighbor.with(HALF, half)
          : Blocks.AIR.getDefaultState();
    }
    if (half == DoubleBlockHalf.LOWER
        && direction == Direction.DOWN
        && !state.canPlaceAt(world, pos)) {
      return Blocks.AIR.getDefaultState();
    }
    return super.getStateForNeighborUpdate(state, direction, neighbor, world, pos, neighborPos);
  }

  @Override
  public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
      BlockPos below = pos.down();
      BlockState under = world.getBlockState(below);
      if (under.isOf(this)) {
        world.setBlockState(
            below, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
        world.syncWorldEvent(
            player, WorldEvents.BLOCK_BROKEN, below, Block.getRawIdFromState(under));
      }
    }
    return super.onBreak(world, pos, state, player);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      LiftActions.use(serverPlayer, lower(pos, state));
    }
    return ActionResult.SUCCESS;
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return CLOSED.get(state.get(FACING));
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return (state.get(OPEN) ? OPENED : CLOSED).get(state.get(FACING));
  }

  @Override
  protected boolean canPathfindThrough(BlockState state, NavigationType type) {
    return type == NavigationType.LAND && state.get(OPEN);
  }
}
