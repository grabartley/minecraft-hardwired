package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.PanelAction;
import com.grahambartley.hardwired.device.PanelDevice;
import com.grahambartley.hardwired.device.PanelKind;
import com.grahambartley.hardwired.device.PanelRow;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.Scene;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public final class SwitchBlock extends MountedDeviceBlock implements PanelDevice {

  @Override
  public boolean isSwitch() {
    return true;
  }

  public static final IntProperty SCENE = IntProperty.of("scene", 0, Scene.MAX_SCENES);

  private final boolean latching;

  public SwitchBlock(Settings settings, double[] floorBox, boolean latching) {
    super(settings, floorBox);
    this.latching = latching;
    setDefaultState(getDefaultState().with(SCENE, 0));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    super.appendProperties(builder);
    builder.add(SCENE);
  }

  @Override
  protected ActionResult onUse(
      BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
    if (player.isSneaking()) {
      if (!world.isClient) {
        cycleScene(state, world, pos, player);
      }
      return ActionResult.SUCCESS;
    }
    if (!latching && state.get(POWERED)) {
      return ActionResult.CONSUME;
    }
    if (!world.isClient) {
      press(state, (ServerWorld) world, pos);
    }
    return ActionResult.SUCCESS;
  }

  private void press(BlockState state, ServerWorld world, BlockPos pos) {
    flip(state, world, pos);
    if (!state.get(POWERED) && state.get(SCENE) > 0) {
      recallScene(world, pos, state.get(SCENE) - 1);
    }
    if (!latching) {
      world.scheduleBlockTick(pos, this, Hardwired.SERVER_CONFIG.device().buttonHoldTicks());
    }
  }

  @Override
  public PanelRow read(ServerWorld world, BlockPos pos, BlockState state) {
    return new PanelRow(pos, PanelKind.SWITCH, state.get(POWERED), Hue.PLAIN, 0, 0, false);
  }

  @Override
  public boolean work(
      ServerWorld world, BlockPos pos, BlockState state, PanelAction action, LampLook look) {
    if (action != PanelAction.TOGGLE || (!latching && state.get(POWERED))) {
      return false;
    }
    press(state, world, pos);
    return true;
  }

  @Override
  protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
    if (!latching && state.get(POWERED)) {
      flip(state, world, pos);
    }
  }

  private static void cycleScene(BlockState state, World world, BlockPos pos, PlayerEntity player) {
    BlockState cycled = state.cycle(SCENE);
    world.setBlockState(pos, cycled, NOTIFY_ALL);
    int scene = cycled.get(SCENE);
    player.sendMessage(
        scene == 0
            ? Text.translatable("message.hardwired.switch_no_scene")
            : Text.translatable("message.hardwired.switch_scene", scene),
        true);
  }

  private static void recallScene(ServerWorld world, BlockPos pos, int slot) {
    WireEngine engine = WireEngine.of(world);
    engine
        .findOnNetwork(pos, ControlPanelBlockEntity.class)
        .flatMap(panel -> Scene.at(panel.scenes(), slot))
        .ifPresent(scene -> engine.applyLook(pos, scene.look()));
  }

  private static void flip(BlockState state, World world, BlockPos pos) {
    BlockState flipped = state.cycle(POWERED);
    world.setBlockState(pos, flipped, NOTIFY_ALL);
    float pitch = flipped.get(POWERED) ? 1.5F : 1.2F;
    world.playSound(
        null, pos, SoundEvents.BLOCK_COPPER_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.6F, pitch);
  }
}
