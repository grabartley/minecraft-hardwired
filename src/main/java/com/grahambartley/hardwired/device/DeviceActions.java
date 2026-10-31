package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.JunctionRule;
import com.grahambartley.hardwired.network.JunctionSetPayload;
import com.grahambartley.hardwired.network.OpenDevicePayload;
import com.grahambartley.hardwired.network.PanelActionPayload;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import com.grahambartley.hardwired.wire.Scene;
import com.grahambartley.hardwired.wire.WireEngine;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class DeviceActions {

  private static final double REACH_MARGIN = 1.0;

  private DeviceActions() {}

  public static ActionResult open(PlayerEntity player, BlockPos pos) {
    if (player instanceof ServerPlayerEntity serverPlayer) {
      ServerPlayNetworking.send(
          serverPlayer, new OpenDevicePayload(pos, lookAt(serverPlayer, pos)));
    }
    return ActionResult.SUCCESS;
  }

  public static void panel(ServerPlayerEntity player, PanelActionPayload action) {
    ServerWorld world = player.getServerWorld();
    if (!canUse(player, action.pos())
        || !(world.getBlockEntity(action.pos()) instanceof ControlPanelBlockEntity panel)) {
      return;
    }
    if (action.slot() >= 0 && action.slot() < Scene.MAX_SCENES) {
      panel.saveScene(action.slot(), action.sceneName(), action.look());
      ModCriteria.DEVICE.trigger(player, ModCriteria.SCENE_SAVED);
      return;
    }
    WireEngine.of(world).applyLook(action.pos(), action.look());
    ModCriteria.triggerAll(player, action.look().color());
  }

  public static void junction(ServerPlayerEntity player, JunctionSetPayload action) {
    if (!canUse(player, action.pos())) {
      return;
    }
    ServerWorld world = player.getServerWorld();
    BlockState state = world.getBlockState(action.pos());
    if (!(state.getBlock() instanceof JunctionBlock)) {
      return;
    }
    JunctionRule[] rules = JunctionRule.values();
    world.setBlockState(
        action.pos(),
        state
            .with(JunctionBlock.RULE, rules[Math.floorMod(action.rule(), rules.length)])
            .with(JunctionBlock.OUTPUT, Hue.byIndex(action.output())),
        Block.NOTIFY_ALL);
  }

  private static LampLook lookAt(ServerPlayerEntity player, BlockPos pos) {
    LampLook look = WireEngine.of(player.getServerWorld()).lookAt(pos);
    return look == null ? LampLook.DEFAULT : look;
  }

  private static boolean canUse(ServerPlayerEntity player, BlockPos pos) {
    return !player.isSpectator()
        && player.canInteractWithBlockAt(pos, REACH_MARGIN)
        && player.getServerWorld().canPlayerModifyAt(player, pos);
  }
}
