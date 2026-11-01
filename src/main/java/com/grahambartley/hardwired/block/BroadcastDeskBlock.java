package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.Electronic;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.radio.DeskActions;
import com.grahambartley.hardwired.radio.DeskRun;
import com.grahambartley.hardwired.radio.StationRegistry;
import com.grahambartley.hardwired.radio.TowerFinder;
import com.grahambartley.hardwired.wire.WireEngine;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class BroadcastDeskBlock extends HorizontalFacingBlock
    implements BlockEntityProvider, WireDevice, Electronic {

  public static final EnumProperty<DeskPart> PART = EnumProperty.of("part", DeskPart.class);
  public static final BooleanProperty ON_AIR = BooleanProperty.of("on_air");
  public static final BooleanProperty DISC = BooleanProperty.of("disc");

  private static final VoxelShape LOW = Block.createCuboidShape(0, 0, 0, 16, 12, 16);

  public BroadcastDeskBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(PART, DeskPart.DESK)
            .with(ON_AIR, false)
            .with(DISC, false));
  }

  public static BlockPos panelOf(BlockPos pos, BlockState state) {
    return DeskLayout.panelOf(state.get(PART), state.get(FACING), pos);
  }

  @Override
  protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
    return MapCodec.unit(this);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, PART, ON_AIR, DISC);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return state.get(PART) == DeskPart.PANEL ? new BroadcastDeskBlockEntity(pos, state) : null;
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    Direction facing = context.getHorizontalPlayerFacing();
    BlockPos panel = context.getBlockPos().offset(facing);
    World world = context.getWorld();
    boolean fits =
        DeskLayout.cells(facing, panel).stream()
            .allMatch(
                cell ->
                    world.isInBuildLimit(cell)
                        && world.getWorldBorder().contains(cell)
                        && world.getBlockState(cell).canReplace(context));
    return fits ? getDefaultState().with(FACING, facing) : null;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    BlockPos panel = panelOf(pos, state);
    world.setBlockState(panel, state.with(PART, DeskPart.PANEL), Block.NOTIFY_ALL);
    world.setBlockState(panel.up(), state.with(PART, DeskPart.TOP), Block.NOTIFY_ALL);
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())) {
      if (world.getBlockEntity(pos) instanceof BroadcastDeskBlockEntity desk) {
        ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), desk.takeDisc());
      }
      for (BlockPos cell : DeskLayout.cells(state.get(FACING), panelOf(pos, state))) {
        if (world.getBlockState(cell).isOf(this)) {
          world.setBlockState(
              cell, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
        }
      }
    }
    super.onStateReplaced(state, world, pos, newState, moved);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return state.get(PART) == DeskPart.PANEL ? VoxelShapes.fullCube() : LOW;
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
    if (!stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
      return ItemActionResult.SUCCESS;
    }
    BlockPos panel = panelOf(pos, state);
    if (!DeskActions.mayWork(serverPlayer, panel)) {
      return ItemActionResult.CONSUME;
    }
    if (!(world.getBlockEntity(panel) instanceof BroadcastDeskBlockEntity desk) || desk.hasDisc()) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    desk.putDisc(stack.splitUnlessCreative(1, player));
    player.incrementStat(Stats.PLAY_RECORD);
    showOnEveryCell(serverPlayer.getServerWorld(), panel, desk);
    return ItemActionResult.CONSUME;
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      DeskActions.open(serverPlayer, panelOf(pos, state));
    }
    return ActionResult.SUCCESS;
  }

  @Override
  public DeviceStatus demand(ServerWorld world, BlockPos pos, BlockState state) {
    if (!(world.getBlockEntity(pos) instanceof BroadcastDeskBlockEntity desk)) {
      return DeviceStatus.READY;
    }
    Optional<TowerFinder.Found> tower = TowerFinder.tallest(world, wiredAt(world, pos, state));
    desk.useTower(tower.orElse(null));
    boolean inUse =
        tower.isPresent() && StationRegistry.of(world).towerUsedByAnother(tower.get().foot(), pos);
    return DeskRun.demand(
        desk.wantsAir(),
        tower.map(TowerFinder.Found::column),
        inUse,
        Hardwired.SERVER_CONFIG.radio());
  }

  @Override
  public int chargePerSecond() {
    return Hardwired.SERVER_CONFIG.power().broadcastChargePerSecond();
  }

  @Override
  public BlockPos wiredAt(ServerWorld world, BlockPos pos, BlockState state) {
    WireEngine engine = WireEngine.of(world);
    return DeskLayout.cells(state.get(FACING), pos).stream()
        .filter(engine::isWired)
        .findFirst()
        .orElse(pos);
  }

  @Override
  public void show(ServerWorld world, BlockPos pos, BlockState state, DeviceStatus status) {
    if (world.getBlockEntity(pos) instanceof BroadcastDeskBlockEntity desk) {
      desk.afterBeat(status, world.getTime());
      desk.publish(world);
      showOnEveryCell(world, pos, desk);
    }
  }

  @Override
  public boolean keepsChecking(DeviceStatus status) {
    return status != DeviceStatus.READY;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    ElectronicsDriver.wake(world, panelOf(pos, state));
  }

  @Override
  protected void onBlockAdded(
      BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
    if (!oldState.isOf(this) && state.get(PART) == DeskPart.PANEL) {
      world.scheduleBlockTick(pos, this, 1);
    }
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    ElectronicsDriver.pulse(world, pos);
  }

  public static void showOnEveryCell(
      ServerWorld world, BlockPos panel, BroadcastDeskBlockEntity desk) {
    boolean onAir = desk.status() == DeviceStatus.RUNNING;
    Direction facing = world.getBlockState(panel).get(FACING);
    for (BlockPos cell : DeskLayout.cells(facing, panel)) {
      BlockState state = world.getBlockState(cell);
      if (state.getBlock() instanceof BroadcastDeskBlock) {
        ElectronicsDriver.show(
            world, cell, state, state.with(ON_AIR, onAir).with(DISC, desk.hasDisc()));
      }
    }
  }
}
