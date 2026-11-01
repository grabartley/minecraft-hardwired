package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.AnimatedDevice;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.power.PowerSource;
import com.grahambartley.hardwired.power.SourceSwitches;
import com.grahambartley.hardwired.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class SourceBlock extends Block
    implements BlockEntityProvider, WireDevice, PowerSource, AnimatedDevice {

  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

  private static final float CLICK_VOLUME = 0.5F;
  private static final float CLICK_PITCH = 0.7F;

  protected SourceBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getPlacementState(ItemPlacementContext context) {
    return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
  }

  @Override
  public boolean isAnimating(BlockState state) {
    return true;
  }

  @Override
  protected void onStateReplaced(
      BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.isOf(newState.getBlock())
        && world.getBlockEntity(pos) instanceof SourceBlockEntity source
        && source.isRunning()) {
      world.playSound(
          null, pos, ModSounds.SOURCE_DOWN, SoundCategory.BLOCKS, source.kind().soundVolume(), 1F);
    }
    super.onStateReplaced(state, world, pos, newState, moved);
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
    return stack.isEmpty()
        ? ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        : ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  public boolean showsGridTotals() {
    return true;
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player instanceof ServerPlayerEntity serverPlayer
        && world instanceof ServerWorld server
        && world.getBlockEntity(pos) instanceof SourceBlockEntity source
        && SourceSwitches.mayWork(serverPlayer, pos)) {
      source.toggleSwitch(server);
      world.playSound(
          null,
          pos,
          SoundEvents.BLOCK_LEVER_CLICK,
          SoundCategory.BLOCKS,
          CLICK_VOLUME,
          CLICK_PITCH);
    }
    return ActionResult.SUCCESS;
  }
}
