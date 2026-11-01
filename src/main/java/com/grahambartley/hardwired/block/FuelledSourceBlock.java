package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.power.CellRules;
import com.grahambartley.hardwired.power.SourceKind;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class FuelledSourceBlock extends SourceBlock {

  public static final IntProperty LEVEL = IntProperty.of("level", 0, CellRules.MAX_LEVEL);

  private static final float FILL_VOLUME = 0.6F;
  private static final float FILL_PITCH = 1.3F;

  protected FuelledSourceBlock(Settings settings) {
    super(settings);
    setDefaultState(getDefaultState().with(LEVEL, 0));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(LEVEL);
  }

  @Override
  public boolean isFuelled(ServerWorld world, BlockPos pos, BlockState state) {
    return world.getBlockEntity(pos) instanceof FuelledSourceBlockEntity source
        && source.charge() > 0;
  }

  @Override
  public void onPlaced(
      World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    if (world.getBlockEntity(pos) instanceof FuelledSourceBlockEntity source) {
      source.showLevel();
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
      return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }
    if (world.getBlockEntity(pos) instanceof FuelledSourceBlockEntity source && !world.isClient) {
      int worth = CellRules.worth(Hardwired.SERVER_CONFIG.power().dustCharge(), redstoneBlock);
      int items =
          CellRules.itemsThatFit(source.charge(), source.capacity(), worth, stack.getCount());
      if (items > 0) {
        fill(source, stack, world, pos, player, items, worth * items);
      } else {
        player.sendMessage(Text.translatable("device.hardwired.cell.no_room"), true);
      }
    }
    return ItemActionResult.success(world.isClient);
  }

  private static void fill(
      FuelledSourceBlockEntity source,
      ItemStack stack,
      World world,
      BlockPos pos,
      PlayerEntity player,
      int items,
      int charge) {
    stack.decrementUnlessCreative(items, player);
    source.fill(charge);
    world.playSound(
        null,
        pos,
        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
        SoundCategory.BLOCKS,
        FILL_VOLUME,
        FILL_PITCH);
    if (world instanceof ServerWorld server) {
      WireEngine.of(server).power().changed(pos);
    }
    if (player instanceof ServerPlayerEntity serverPlayer && source.kind() == SourceKind.CELL) {
      ModCriteria.DEVICE.trigger(serverPlayer, ModCriteria.CELL_FILLED);
    }
  }
}
