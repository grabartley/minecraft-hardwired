package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.power.SourceSwitches;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

public final class MasterSwitchBlock extends Block implements BlockEntityProvider, WireDevice {

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
  public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
  public static final BooleanProperty CLOSED = BooleanProperty.of("closed");
  public static final BooleanProperty LIVE = BooleanProperty.of("live");

  private static final double DEPTH = 6D;
  private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

  static {
    SHAPES.put(Direction.NORTH, Block.createCuboidShape(0, 0, 16 - DEPTH, 16, 16, 16));
    SHAPES.put(Direction.SOUTH, Block.createCuboidShape(0, 0, 0, 16, 16, DEPTH));
    SHAPES.put(Direction.WEST, Block.createCuboidShape(16 - DEPTH, 0, 0, 16, 16, 16));
    SHAPES.put(Direction.EAST, Block.createCuboidShape(0, 0, 0, DEPTH, 16, 16));
  }

  public MasterSwitchBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(HALF, DoubleBlockHalf.LOWER)
            .with(CLOSED, false)
            .with(LIVE, false));
  }

  public static void setClosed(
      ServerWorld world, BlockPos lower, BlockState state, boolean closed) {
    world.setBlockState(lower, state.with(CLOSED, closed), NOTIFY_ALL);
    BlockState upper = world.getBlockState(lower.up());
    if (upper.getBlock() instanceof MasterSwitchBlock) {
      world.setBlockState(lower.up(), upper.with(CLOSED, closed), NOTIFY_ALL);
    }
  }

  public static void showLive(ServerWorld world, BlockPos lower, boolean live) {
    for (BlockPos half : List.of(lower, lower.up())) {
      BlockState state = world.getBlockState(half);
      if (state.getBlock() instanceof MasterSwitchBlock && state.get(LIVE) != live) {
        world.setBlockState(half, state.with(LIVE, live), NOTIFY_LISTENERS);
      }
    }
  }

  public boolean isClosed(BlockState state) {
    return state.get(CLOSED);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, HALF, CLOSED, LIVE);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPES.get(state.get(FACING));
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    Direction side = context.getSide();
    BlockPos pos = context.getBlockPos();
    if (!side.getAxis().isHorizontal()
        || !context.getWorld().getBlockState(pos.up()).canReplace(context)) {
      return null;
    }
    return getDefaultState().with(FACING, side);
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), NOTIFY_ALL);
  }

  @Override
  protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    BlockPos wall = pos.offset(state.get(FACING).getOpposite());
    return world.getBlockState(wall).isSideSolidFullSquare(world, wall, state.get(FACING))
        || state.get(HALF) == DoubleBlockHalf.UPPER;
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
    boolean towardsTwin =
        direction == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
    if (towardsTwin && !neighbor.isOf(this)) {
      return Blocks.AIR.getDefaultState();
    }
    if (half == DoubleBlockHalf.LOWER
        && direction == state.get(FACING).getOpposite()
        && !state.canPlaceAt(world, pos)) {
      return Blocks.AIR.getDefaultState();
    }
    return state;
  }

  @Override
  public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
      BlockPos lower = pos.down();
      if (world.getBlockState(lower).isOf(this)) {
        world.setBlockState(lower, Blocks.AIR.getDefaultState(), NOTIFY_ALL | SKIP_DROPS);
        world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, lower, getRawIdFromState(state));
      }
    }
    return super.onBreak(world, pos, state, player);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    BlockPos lower = loadAnchor(pos, state);
    if (player instanceof ServerPlayerEntity serverPlayer
        && world instanceof ServerWorld server
        && world.getBlockEntity(lower) instanceof MasterSwitchBlockEntity master
        && SourceSwitches.mayWork(serverPlayer, lower)) {
      master.requestThrow(server, world.getBlockState(lower));
    }
    return ActionResult.SUCCESS;
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    return state.get(HALF) == DoubleBlockHalf.UPPER
        ? List.of(pos.down(), pos)
        : List.of(pos, pos.up());
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(HALF) == DoubleBlockHalf.LOWER
        ? new MasterSwitchBlockEntity(pos, state)
        : null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world, BlockState state, BlockEntityType<T> type) {
    return type == ModBlockEntities.MASTER_SWITCH && !world.isClient
        ? (BlockEntityTicker<T>)
            (BlockEntityTicker<MasterSwitchBlockEntity>) MasterSwitchBlockEntity::tick
        : null;
  }
}
