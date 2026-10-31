package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.WireDevice;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public abstract class TableDeviceBlock extends HorizontalFacingBlock
    implements BlockEntityProvider, WireDevice, AnimatedDevice, Electronic {

  private final VoxelShape shape;

  protected TableDeviceBlock(Settings settings, double[] box) {
    super(settings);
    this.shape = Block.createCuboidShape(box[0], box[1], box[2], box[3], box[4], box[5]);
    setDefaultState(
        getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(live(), false));
  }

  protected abstract BooleanProperty live();

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, live());
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
    return shape;
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return state.get(live());
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    ElectronicsDriver.wake(world, pos);
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, 1);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (!world.isClient) {
      ElectronicsDriver.tell(world, pos, player);
    }
    return ActionResult.SUCCESS;
  }
}
