package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.lift.LiftGateCells;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class LargeLiftFloorBlock extends LiftFloorBlock {

  public static final BooleanProperty SECOND = BooleanProperty.of("second");

  public LargeLiftFloorBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(SECOND, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(SECOND);
  }

  @Override
  public float dialPivotX() {
    return 1F;
  }

  @Override
  public Identifier leafModel() {
    return Identifier.of(Hardwired.MOD_ID, "block/lift_gate_leaf_wide");
  }

  @Override
  public BlockPos origin(BlockPos pos, BlockState state) {
    return LiftGateCells.origin(super.origin(pos, state), state.get(FACING), state.get(SECOND));
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    return LiftGateCells.cells(origin(pos, state), state.get(FACING));
  }

  @Override
  public List<BlockPos> columns(BlockPos pos, BlockState state) {
    return LiftGateCells.columns(origin(pos, state), state.get(FACING));
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    Direction facing = context.getHorizontalPlayerFacing();
    BlockPos pos = context.getBlockPos();
    boolean aimed = LiftGateCells.placedAsSecond(pos, facing, context.getHitPos());
    BlockState state = placed(context, facing, aimed);
    return state != null ? state : placed(context, facing, !aimed);
  }

  private BlockState placed(ItemPlacementContext context, Direction facing, boolean second) {
    BlockState state = getDefaultState().with(FACING, facing).with(SECOND, second);
    BlockPos origin = LiftGateCells.origin(context.getBlockPos(), facing, second);
    List<BlockPos> cells = LiftGateCells.cells(origin, facing);
    World world = context.getWorld();
    boolean stands = state.canPlaceAt(world, cells.get(0)) && state.canPlaceAt(world, cells.get(2));
    return stands && WireDevice.fits(context, state, cells) ? state : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    List<BlockPos> cells = cells(pos, state);
    for (int cell = 0; cell < cells.size(); cell++) {
      if (!cells.get(cell).equals(pos)) {
        world.setBlockState(
            cells.get(cell),
            state
                .with(SECOND, cell >= 2)
                .with(HALF, cell % 2 == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER),
            Block.NOTIFY_ALL);
      }
    }
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
      WireDevice.clearOthers(world, pos, this, cells(pos, state));
    }
    super.onStateReplaced(state, world, pos, next, moved);
  }
}
