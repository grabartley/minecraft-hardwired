package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.ControlPanelBlockEntity;
import com.grahambartley.hardwired.block.JunctionBlock;
import com.grahambartley.hardwired.block.JunctionRule;
import com.grahambartley.hardwired.network.JunctionSetPayload;
import com.grahambartley.hardwired.network.PanelActionPayload;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.Scene;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class DeviceActions {

  private DeviceActions() {}

  public static void panel(ServerPlayerEntity player, PanelActionPayload action) {
    ServerWorld world = player.getServerWorld();
    if (!reachable(player, action.pos())
        || !(world.getBlockEntity(action.pos()) instanceof ControlPanelBlockEntity panel)) {
      return;
    }
    if (action.slot() >= 0 && action.slot() < Scene.MAX_SCENES) {
      panel.saveScene(action.slot(), action.sceneName());
      ModCriteria.DEVICE.trigger(player, ModCriteria.SCENE_SAVED);
      return;
    }
    panel.setLook(action.look());
    WireEngine.of(world).applyLook(action.pos(), action.look());
    ModCriteria.triggerAll(player, action.look().color());
  }

  public static void junction(ServerPlayerEntity player, JunctionSetPayload action) {
    ServerWorld world = player.getServerWorld();
    BlockState state = world.getBlockState(action.pos());
    if (!reachable(player, action.pos()) || !(state.getBlock() instanceof JunctionBlock)) {
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

  private static boolean reachable(ServerPlayerEntity player, BlockPos pos) {
    return player.canInteractWithBlockAt(pos, 1.0);
  }
}
