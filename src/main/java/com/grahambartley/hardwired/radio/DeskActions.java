package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.BroadcastDeskBlock;
import com.grahambartley.hardwired.block.BroadcastDeskBlockEntity;
import com.grahambartley.hardwired.block.DeskLayout;
import com.grahambartley.hardwired.device.ActionRate;
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

  private static final int MIN_TICKS_BETWEEN_AIR_TOGGLES = 4;
  private static final ActionRate AIR_TOGGLES = new ActionRate(MIN_TICKS_BETWEEN_AIR_TOGGLES);
  private static final ActionRate SONG_PRESSES = new ActionRate(MIN_TICKS_BETWEEN_AIR_TOGGLES);

  private DeskActions() {}

  public static void forget(ServerPlayerEntity player) {
    AIR_TOGGLES.forget(player.getUuid());
    SONG_PRESSES.forget(player.getUuid());
  }

  public static boolean mayWork(ServerPlayerEntity player, BlockPos panel) {
    if (!player.canModifyBlocks() || !DeviceActions.canUse(player, panel)) {
      return false;
    }
    ServerWorld world = player.getServerWorld();
    BlockState state = world.getBlockState(panel);
    if (!(state.getBlock() instanceof BroadcastDeskBlock)) {
      return false;
    }
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
    BlockPos panel = action.pos();
    DeskAction asked = DeskAction.byIndex(action.action());
    ServerWorld world = player.getServerWorld();
    if (asked == null
        || !mayWork(player, panel)
        || !(world.getBlockEntity(panel) instanceof BroadcastDeskBlockEntity desk)) {
      return;
    }
    switch (asked) {
      case SET_FREQUENCY -> {
        if (Frequency.snap(action.value()) != desk.frequency()) {
          desk.setFrequency(action.value());
          desk.publish(world);
        }
      }
      case TOGGLE_AIR -> toggleAir(player, world, desk);
      case PLAY -> {
        if (SONG_PRESSES.allow(player.getUuid(), player.getServer().getTicks())) {
          play(player, world, desk);
        }
      }
      case STOP -> {
        if (SONG_PRESSES.allow(player.getUuid(), player.getServer().getTicks())) {
          desk.pauseSong(world.getTime());
          desk.publish(world);
          BroadcastDeskBlock.showOnEveryCell(world, panel, desk);
        }
      }
      case RESTART -> {
        if (SONG_PRESSES.allow(player.getUuid(), player.getServer().getTicks())) {
          desk.stopSong();
          play(player, world, desk);
        }
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

  private static void toggleAir(
      ServerPlayerEntity player, ServerWorld world, BroadcastDeskBlockEntity desk) {
    if (AIR_TOGGLES.allow(player.getUuid(), player.getServer().getTicks())) {
      desk.setWantsAir(!desk.wantsAir());
      ElectronicsDriver.pulse(world, desk.getPos());
    }
  }

  private static void play(
      ServerPlayerEntity player, ServerWorld world, BroadcastDeskBlockEntity desk) {
    if (desk.status() == DeviceStatus.RUNNING && desk.song().isPresent() && !desk.songPlaying()) {
      desk.startSong(world.getTime());
      desk.publish(world);
      BroadcastDeskBlock.showOnEveryCell(world, desk.getPos(), desk);
      ModCriteria.DEVICE.trigger(player, ModCriteria.ON_THE_AIR);
    }
  }
}
