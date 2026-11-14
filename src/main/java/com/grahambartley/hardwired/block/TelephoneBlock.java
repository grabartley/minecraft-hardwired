package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.PhoneDevice;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.phone.PhoneActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.phone.PhoneLifecycle;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class TelephoneBlock extends HorizontalFacingBlock
    implements PhoneDevice, BlockEntityProvider, WireDevice, AnimatedDevice, Electronic {

  private final double[] floorBox;
  private final BlockFace mount;

  public TelephoneBlock(Settings settings, double[] floorBox, BlockFace mount) {
    super(settings);
    this.floorBox = floorBox;
    this.mount = mount;
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(Mounting.FACE, mount)
            .with(FACING, Direction.NORTH)
            .with(LIFTED, false)
            .with(RINGING, false));
  }

  @Override
  protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(Mounting.FACE, FACING, LIFTED, RINGING);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ElectronicBlockEntity(pos, state);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    return Mounting.placement(getDefaultState(), Set.of(mount), context);
  }

  @Override
  protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    return Mounting.canPlaceAt(state, world, pos);
  }

  @Override
  protected BlockState getStateForNeighborUpdate(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      WorldAccess world,
      BlockPos pos,
      BlockPos neighborPos) {
    return Mounting.afterNeighborChange(state, direction, world, pos);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return MountShapes.shape(floorBox, state.get(Mounting.FACE), state.get(FACING));
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    PhoneLifecycle.placed(world, pos, placer, stack);
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) {
      PhoneLifecycle.removed(world, pos);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      PhoneActions.use(serverPlayer, pos);
    }
    return ActionResult.SUCCESS;
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this)) {
      world.scheduleBlockTick(pos, this, 1);
      PhoneLifecycle.listed(world, pos, List.of(pos), false);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    PhoneLifecycle.rewired(world, pos);
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return state.get(RINGING);
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return PhoneDirectory.readiness(world, pos);
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {}

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().telephoneLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return loadWhenWired(world, pos, state);
  }
}
