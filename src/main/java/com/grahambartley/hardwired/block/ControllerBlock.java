package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.device.PanelDevice;
import com.grahambartley.hardwired.device.PanelKind;
import com.grahambartley.hardwired.device.PanelRow;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireEngine;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class ControllerBlock extends HorizontalFacingBlock
    implements BlockEntityProvider, WireDevice, PanelDevice, AnimatedDevice {

  public static final BooleanProperty RUNNING = BooleanProperty.of("running");

  public ControllerBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(RUNNING, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, RUNNING);
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
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new ControllerBlockEntity(pos, state);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      World world, BlockState state, BlockEntityType<T> type) {
    if (world.isClient || !state.get(RUNNING) || type != ModBlockEntities.CONTROLLER) {
      return null;
    }
    return (BlockEntityTicker<T>)
        (BlockEntityTicker<ControllerBlockEntity>) ControllerBlockEntity::tick;
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    return DeviceActions.open(player, pos);
  }

  @Override
  public boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return world.getBlockEntity(pos) instanceof ControllerBlockEntity controller
        && controller.drives(color);
  }

  @Override
  public int ratedLoad(PowerSettings settings) {
    return settings.loads().logicLoad();
  }

  @Override
  public int load(ServerWorld world, BlockPos pos, BlockState state) {
    return WireEngine.of(world).isOn(pos) ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    if (world.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
      controller.readWires(world);
    }
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return state.get(RUNNING);
  }

  @Override
  public PanelRow read(ServerWorld world, BlockPos pos, BlockState state) {
    if (world.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
      return new PanelRow(
          pos,
          PanelKind.CONTROLLER,
          state.get(RUNNING),
          Hue.PLAIN,
          controller.run().position(),
          controller.program().timeline().size(),
          false);
    }
    return new PanelRow(pos, PanelKind.CONTROLLER, false, Hue.PLAIN, 0, 0, false);
  }

  @Override
  public boolean work(
      ServerWorld world, BlockPos pos, BlockState state, PanelAction action, LampLook look) {
    if (action == PanelAction.TOGGLE
        && world.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
      controller.toggleRun();
      return true;
    }
    return false;
  }
}
