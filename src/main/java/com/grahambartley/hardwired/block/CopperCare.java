package com.grahambartley.hardwired.block;

import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CopperCare {

  public static final IntProperty AGE = IntProperty.of("age", 0, 3);
  public static final BooleanProperty WAXED = BooleanProperty.of("waxed");
  public static final int MAX_AGE = 3;
  private static final float BASE_AGE_CHANCE = 64F / 1125F;
  private static final float PERCENT = 100F;

  private CopperCare() {}

  public static boolean canAge(BlockState state) {
    return state.get(AGE) < MAX_AGE && !state.get(WAXED);
  }

  public static float ageChance(int ratePercent) {
    return BASE_AGE_CHANCE * ratePercent / PERCENT;
  }

  public static ItemActionResult use(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      Consumer<BlockState> update) {
    if (stack.isOf(Items.HONEYCOMB) && !state.get(WAXED)) {
      stack.decrementUnlessCreative(1, player);
      return change(world, pos, state.with(WAXED, true), SoundEvents.ITEM_HONEYCOMB_WAX_ON, update);
    }
    if (stack.getItem() instanceof AxeItem) {
      return scrape(stack, state, world, pos, player, hand, update);
    }
    return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  private static ItemActionResult scrape(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      Consumer<BlockState> update) {
    if (state.get(WAXED)) {
      stack.damage(1, player, PlayerEntity.getSlotForHand(hand));
      return change(world, pos, state.with(WAXED, false), SoundEvents.ITEM_AXE_WAX_OFF, update);
    }
    if (state.get(AGE) > 0) {
      stack.damage(1, player, PlayerEntity.getSlotForHand(hand));
      return change(
          world, pos, state.with(AGE, state.get(AGE) - 1), SoundEvents.ITEM_AXE_SCRAPE, update);
    }
    return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  private static ItemActionResult change(
      World world, BlockPos pos, BlockState state, SoundEvent sound, Consumer<BlockState> update) {
    if (!world.isClient) {
      update.accept(state);
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1F, 1F);
    }
    return ItemActionResult.SUCCESS;
  }
}
