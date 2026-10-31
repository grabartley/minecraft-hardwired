package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.power.CellRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class RedstoneCellBlock extends Block implements BlockEntityProvider, WireDevice {

  public static final IntProperty LEVEL = IntProperty.of("level", 0, CellRules.MAX_LEVEL);

  private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 14, 14);
  private static final float FILL_VOLUME = 0.6F;
  private static final float FILL_PITCH = 1.3F;

  public RedstoneCellBlock(Settings settings) {
    super(settings);
    setDefaultState(getStateManager().getDefaultState().with(LEVEL, 0));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(LEVEL);
  }

  @Override
  protected VoxelShape getOutlineShape(
      BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
    return SHAPE;
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new RedstoneCellBlockEntity(pos, state);
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    if (world.getBlockEntity(pos) instanceof RedstoneCellBlockEntity cell) {
      cell.showLevel();
    }
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
    boolean redstoneBlock = stack.isOf(Items.REDSTONE_BLOCK);
    if (!redstoneBlock && !stack.isOf(Items.REDSTONE)) {
      return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    if (world.getBlockEntity(pos) instanceof RedstoneCellBlockEntity cell && !world.isClient) {
      int worth = CellRules.worth(Hardwired.SERVER_CONFIG.power().dustCharge(), redstoneBlock);
      if (CellRules.takes(cell.charge(), cell.capacity(), worth)) {
        fill(cell, stack, world, pos, player, worth);
      } else {
        player.sendMessage(Text.translatable("device.hardwired.cell.no_room"), true);
      }
    }
    return ItemActionResult.success(world.isClient);
  }

  private static void fill(
      RedstoneCellBlockEntity cell,
      ItemStack stack,
      World world,
      BlockPos pos,
      PlayerEntity player,
      int worth) {
    stack.decrementUnlessCreative(1, player);
    cell.fill(worth);
    world.playSound(
        null,
        pos,
        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
        SoundCategory.BLOCKS,
        FILL_VOLUME,
        FILL_PITCH);
    if (player instanceof ServerPlayerEntity serverPlayer) {
      ModCriteria.DEVICE.trigger(serverPlayer, ModCriteria.CELL_FILLED);
    }
  }
}
