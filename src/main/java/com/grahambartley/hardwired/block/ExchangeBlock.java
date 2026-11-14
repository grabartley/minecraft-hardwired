package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.phone.ExchangeActions;
import com.grahambartley.hardwired.phone.PhoneDirectory;
import com.grahambartley.hardwired.wire.WireEngine;
import com.mojang.serialization.MapCodec;
import java.util.List;
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
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class ExchangeBlock extends HorizontalFacingBlock
    implements BlockEntityProvider, WireDevice, Electronic {

  public static final EnumProperty<ExchangePart> PART = EnumProperty.of("part", ExchangePart.class);
  private static final double[] BOARD_BOX = {0, 0, 6, 16, 16, 16};

  public ExchangeBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(PART, ExchangePart.LOWER_LEFT));
  }

  public static BlockPos anchorOf(BlockPos pos, BlockState state) {
    return ExchangeLayout.anchorOf(state.get(PART), state.get(FACING), pos);
  }

  @Override
  protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, PART);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(PART) == ExchangePart.LOWER_LEFT
        ? new ElectronicBlockEntity(pos, state)
        : null;
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    BlockState state =
        getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    return WireDevice.fits(context, state, cells(context.getBlockPos(), state)) ? state : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    Direction facing = state.get(FACING);
    for (ExchangePart part : ExchangePart.values()) {
      if (part != ExchangePart.LOWER_LEFT) {
        world.setBlockState(
            ExchangeLayout.cell(part, facing, pos), state.with(PART, part), Block.NOTIFY_ALL);
      }
    }
    if (world instanceof ServerWorld server && placer instanceof ServerPlayerEntity player) {
      PhoneDirectory.of(server).placedBy(pos.asLong(), "", player.getUuid());
    }
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) {
      BlockPos anchor = anchorOf(pos, state);
      WireDevice.clearOthers(world, pos, this, cells(pos, state));
      if (world instanceof ServerWorld server) {
        PhoneDirectory.of(server).remove(anchor.asLong());
      }
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return MountShapes.shape(BOARD_BOX, BlockFace.FLOOR, state.get(FACING));
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      ExchangeActions.open(serverPlayer, anchorOf(pos, state));
    }
    return ActionResult.SUCCESS;
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this) && state.get(PART) == ExchangePart.LOWER_LEFT) {
      world.scheduleBlockTick(pos, this, 1);
      if (world instanceof ServerWorld server) {
        PhoneDirectory.of(server)
            .add(pos.asLong(), cells(pos, state).stream().map(BlockPos::asLong).toList(), true);
      }
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    PhoneDirectory.of(world).markStale();
    ElectronicsDriver.wake(world, anchorOf(pos, state));
  }

  @Override
  public List<BlockPos> cells(BlockPos pos, BlockState state) {
    return ExchangeLayout.cells(state.get(FACING), anchorOf(pos, state));
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    return PhoneDirectory.readiness(world, pos);
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {}

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().exchangeLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    WireEngine engine = WireEngine.of(world);
    boolean wired = cells(pos, state).stream().anyMatch(engine::isWired);
    return wired ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }
}
