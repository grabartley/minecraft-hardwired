package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public abstract class AbstractLampBlock extends Block implements BlockEntityProvider, WireDevice {

  public static final IntProperty LEVEL = IntProperty.of("level", 0, 15);
  public static final BooleanProperty LIT = Properties.LIT;
  public static final IntProperty AGE = IntProperty.of("age", 0, 3);
  public static final BooleanProperty WAXED = BooleanProperty.of("waxed");
  public static final int MAX_AGE = 3;
  private static final float BASE_AGE_CHANCE = 64F / 1125F;

  protected AbstractLampBlock(Settings settings) {
    super(settings);
    setDefaultState(
        getStateManager()
            .getDefaultState()
            .with(LEVEL, 0)
            .with(LIT, false)
            .with(AGE, 0)
            .with(WAXED, false));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(LEVEL, LIT, AGE, WAXED);
  }

  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new LampBlockEntity(pos, state);
  }

  public static boolean isGlowing(BlockState state, BlockView world, BlockPos pos) {
    return state.get(LIT);
  }

  public static int lightLevel(BlockState state) {
    return state.get(LEVEL);
  }

  public BlockPos lampPos(BlockPos pos, BlockState state) {
    return pos;
  }

  protected List<BlockPos> parts(BlockPos pos, BlockState state) {
    return List.of(pos);
  }

  protected void update(World world, BlockPos pos, BlockState state) {
    world.setBlockState(pos, state, NOTIFY_ALL);
  }

  @Override
  public void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {
    BlockPos lampPos = lampPos(pos, state);
    if (!(world.getBlockEntity(lampPos) instanceof LampBlockEntity lamp)) {
      return;
    }
    long wait = LampRateLimiter.ticksUntilAllowed(lamp.lastChangeTick(), world.getTime());
    if (wait > 0) {
      world.scheduleBlockTick(pos, this, (int) wait);
      return;
    }
    WireEngine engine = WireEngine.of(world);
    List<BlockPos> parts = parts(pos, state);
    LampLook look =
        parts.stream().map(engine::lookAt).filter(Objects::nonNull).findFirst().orElse(null);
    Hue color = look == null ? lamp.color() : look.color();
    int brightness = look == null ? LampLook.MAX_BRIGHTNESS : look.brightness();
    boolean changed = lamp.setLook(color, brightness);
    int level = parts.stream().anyMatch(engine::isPowered) ? brightness : 0;
    if (state.get(LEVEL) != level) {
      update(world, pos, state.with(LEVEL, level).with(LIT, level > 0));
      changed = true;
      if (level > 0) {
        ModCriteria.DEVICE.triggerNear(world, pos, litEvent(world));
      }
    }
    if (changed) {
      lamp.markChanged(world.getTime());
    }
  }

  protected String litEvent(World world) {
    return ModCriteria.LAMP_LIT;
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    onWireChange(world, pos, state);
  }

  @Override
  protected boolean hasRandomTicks(BlockState state) {
    return state.get(AGE) < MAX_AGE && !state.get(WAXED);
  }

  @Override
  protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (random.nextFloat()
        < BASE_AGE_CHANCE * Hardwired.SERVER_CONFIG.lamp().ageRateMultiplierPercent() / 100F) {
      int age = state.get(AGE) + 1;
      update(world, pos, state.with(AGE, age));
      if (age == MAX_AGE) {
        ModCriteria.DEVICE.triggerNear(world, pos, ModCriteria.LAMP_AGED);
      }
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
    if (stack.getItem() instanceof DyeItem dye) {
      return applyDye(stack, dye, world, lampPos(pos, state), player);
    }
    if (stack.isOf(Items.HONEYCOMB) && !state.get(WAXED)) {
      stack.decrementUnlessCreative(1, player);
      return change(world, pos, state.with(WAXED, true), SoundEvents.ITEM_HONEYCOMB_WAX_ON);
    }
    if (stack.getItem() instanceof AxeItem) {
      return scrape(stack, state, world, pos, player, hand);
    }
    return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  private ItemActionResult applyDye(
      ItemStack stack, DyeItem dye, World world, BlockPos pos, PlayerEntity player) {
    if (world.isClient) {
      return ItemActionResult.SUCCESS;
    }
    Hue hue = Hue.ofDye(dye.getColor());
    if (world.getBlockEntity(pos) instanceof LampBlockEntity lamp
        && lamp.setLook(hue, lamp.brightness())) {
      stack.decrementUnlessCreative(1, player);
      ModCriteria.DEVICE.trigger((ServerPlayerEntity) player, ModCriteria.colorEvent(hue));
      world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1F, 1F);
    }
    return ItemActionResult.SUCCESS;
  }

  private ItemActionResult scrape(
      ItemStack stack,
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand) {
    if (state.get(WAXED)) {
      stack.damage(1, player, PlayerEntity.getSlotForHand(hand));
      return change(world, pos, state.with(WAXED, false), SoundEvents.ITEM_AXE_WAX_OFF);
    }
    if (state.get(AGE) > 0) {
      stack.damage(1, player, PlayerEntity.getSlotForHand(hand));
      return change(world, pos, state.with(AGE, state.get(AGE) - 1), SoundEvents.ITEM_AXE_SCRAPE);
    }
    return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  private ItemActionResult change(World world, BlockPos pos, BlockState state, SoundEvent sound) {
    if (!world.isClient) {
      update(world, pos, state);
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1F, 1F);
    }
    return ItemActionResult.SUCCESS;
  }
}
