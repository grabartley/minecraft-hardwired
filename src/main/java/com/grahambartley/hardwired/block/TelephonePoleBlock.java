package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.line.PoleActions;
import com.grahambartley.hardwired.line.PoleLines;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public final class TelephonePoleBlock extends Block {

  public static final EnumProperty<PolePart> PART = EnumProperty.of("part", PolePart.class);
  public static final EnumProperty<PoleWood> WOOD = EnumProperty.of("wood", PoleWood.class);
  private static final VoxelShape POST = Block.createCuboidShape(5, 0, 5, 11, 16, 11);
  private static final VoxelShape ARM_X =
      VoxelShapes.union(POST, Block.createCuboidShape(0, 11, 6, 16, 14, 10));
  private static final VoxelShape ARM_Z =
      VoxelShapes.union(POST, Block.createCuboidShape(6, 11, 0, 10, 14, 16));

  public TelephonePoleBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(PART, PolePart.SINGLE)
            .with(WOOD, PoleWood.OAK)
            .with(Properties.HORIZONTAL_AXIS, Direction.Axis.X));
  }

  public static boolean isPole(BlockState state) {
    return state.getBlock() instanceof TelephonePoleBlock;
  }

  private static PolePart partAt(BlockView world, BlockPos pos) {
    return PolePart.between(
        isPole(world.getBlockState(pos.down())), isPole(world.getBlockState(pos.up())));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(PART, WOOD, Properties.HORIZONTAL_AXIS);
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
  protected ItemActionResult onUseWithItem(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      BlockHitResult hit) {
    boolean waxing = stack.isOf(Items.HONEYCOMB) && !WireItem.isHolding(player);
    if (!(stack.getItem() instanceof WireItem) && !waxing) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    if (player instanceof ServerPlayerEntity server) {
      boolean used =
          waxing ? PoleActions.wax(server, pos, stack) : PoleActions.use(server, pos, stack);
      return used ? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    return ItemActionResult.SUCCESS;
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
