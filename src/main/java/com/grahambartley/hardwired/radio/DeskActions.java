package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.DeskLayout;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.network.DeskActionPayload;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireViewer;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class DeskActions {

  private DeskActions() {}

  public static boolean mayWork(ServerPlayerEntity player, BlockPos panel) {
    if (!player.canModifyBlocks() || !DeviceActions.canUse(player, panel)) {
      return false;
    }
    ServerWorld world = player.getServerWorld();
    BlockState state = world.getBlockState(panel);
    WireViewer viewer = WireViewer.of(player);
    boolean mayWork =
        DeskLayout.cells(state.get(BroadcastDeskBlock.FACING), panel).stream()
            .flatMap(cell -> WireEngine.of(world).grid().networksTouching(cell).stream())
            .allMatch(viewer::canSee);
    if (!mayWork) {
      player.sendMessage(Text.translatable("message.hardwired.desk_sealed"), true);
    }
    return mayWork;
  }

  public static void open(ServerPlayerEntity player, BlockPos panel) {
    if (mayWork(player, panel)) {
      DeviceActions.open(player, panel);
    }
  }

  public static void act(ServerPlayerEntity player, DeskActionPayload action) {
    ServerWorld world = player.getServerWorld();
    BlockPos panel = action.pos();
    if (!(world.getBlockEntity(panel) instanceof BroadcastDeskBlockEntity desk)
        || !mayWork(player, panel)) {
      return;
    }
    switch (DeskAction.byIndex(action.action())) {
      case SET_FREQUENCY -> {
        desk.setFrequency(action.value());
        desk.publish(world);
      }
      case TOGGLE_AIR -> {
        desk.setWantsAir(!desk.wantsAir());
        ElectronicsDriver.pulse(world, panel);
      }
      case PLAY -> play(player, world, desk);
      case STOP -> {
        desk.stopSong();
        desk.publish(world);
      }
      case EJECT -> {
        player.getInventory().offerOrDrop(desk.takeDisc());
        desk.publish(world);
        BroadcastDeskBlock.showOnEveryCell(world, panel, desk);
      }
      case TAKE_MIC -> DeskVoice.take(player, desk);
      case RELEASE_MIC -> DeskVoice.release(player, desk);
    }
  }

  private static void play(
      ServerPlayerEntity player, ServerWorld world, BroadcastDeskBlockEntity desk) {
    if (desk.status() == DeviceStatus.RUNNING && desk.song().isPresent()) {
      desk.startSong(world.getTime());
      desk.publish(world);
      ModCriteria.DEVICE.trigger(player, ModCriteria.ON_THE_AIR);
    }
  }
}
