package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.advancement.DeviceCriterion;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.MainPanelBlockEntity;
import com.grahambartley.hardwired.network.MainPanelActionPayload;
import com.grahambartley.hardwired.network.MainPanelViewPayload;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireViewer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public final class MainPanelActions {

  private static final PanelRates RATES = new PanelRates();
  private static final float CLICK_VOLUME = 0.4F;
  private static final float CLICK_PITCH = 1.3F;

  private MainPanelActions() {}

  public static void open(ServerPlayerEntity player, BlockPos panel) {
    if (DeviceStops.tell(player.getServerWorld(), panel, player)) {
      return;
    }
    if (RATES.allowOpen(player.getUuid(), player.getServer().getTicks())) {
      send(player, panel, true);
    }
  }

  public static void forget(ServerPlayerEntity player) {
    RATES.forget(player.getUuid());
  }

  public static void act(ServerPlayerEntity player, MainPanelActionPayload action) {
    if (RATES.allow(player.getUuid(), action.action(), player.getServer().getTicks())
        && perform(player, action)) {
      send(player, action.panel(), false);
    }
  }

  public static boolean perform(ServerPlayerEntity player, MainPanelActionPayload action) {
    if (!DeviceActions.canUse(player, action.panel())
        || !(player.getServerWorld().getBlockEntity(action.panel()) instanceof MainPanelBlockEntity)
        || !WireEngine.of(player.getServerWorld()).power().hasPower(action.panel())) {
      return false;
    }
    if (action.action() != PanelAction.REFRESH && work(player, action)) {
      player
          .getServerWorld()
          .playSound(
              null,
              action.panel(),
              SoundEvents.BLOCK_LEVER_CLICK,
              SoundCategory.BLOCKS,
              CLICK_VOLUME,
              CLICK_PITCH);
    }
    return true;
  }

  private static boolean work(ServerPlayerEntity player, MainPanelActionPayload action) {
    ServerWorld world = player.getServerWorld();
    WireEngine engine = WireEngine.of(world);
    BlockPos target = action.target();
    if (!player.canModifyBlocks()
        || !PanelRoster.reaches(engine, action.panel(), target, WireViewer.of(player))
        || !engine.isLoaded(target)
        || !world.canPlayerModifyAt(player, target)) {
      return false;
    }
    BlockState state = world.getBlockState(target);
    if (!(state.getBlock() instanceof PanelDevice device)
        || !device.work(world, target, state, action.action(), action.look())) {
      return false;
    }
    if (action.action() == PanelAction.SET_LOOK) {
      ModCriteria.triggerAll(player, action.look().color());
    }
    recordWorked(player, world, device.anchor(target, state));
    return true;
  }

  private static void recordWorked(ServerPlayerEntity player, ServerWorld world, BlockPos device) {
    String key = world.getRegistryKey().getValue() + "|" + device.asLong();
    int worked = SmartHomeLog.of(world.getServer()).record(player.getUuid(), key);
    for (int count = 1; count <= worked; count++) {
      ModCriteria.DEVICE.trigger(player, DeviceCriterion.panelDeviceEvent(count));
    }
  }

  private static void send(ServerPlayerEntity player, BlockPos panel, boolean open) {
    ServerWorld world = player.getServerWorld();
    if (world.getBlockEntity(panel) instanceof MainPanelBlockEntity entity) {
      PanelRoster.Listing listing =
          PanelRoster.list(world, panel, WireViewer.of(player), entity.memory());
      if (listing.memoryChanged()) {
        entity.markDirty();
      }
      ServerPlayNetworking.send(
          player,
          new MainPanelViewPayload(
              panel, open, listing.rows(), listing.unknownPoints(), listing.omitted()));
    }
  }
}
