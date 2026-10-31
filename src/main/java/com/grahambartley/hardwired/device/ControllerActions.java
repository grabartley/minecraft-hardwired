package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.network.ControllerSetPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ControllerActions {

  private static final int MIN_TICKS_BETWEEN_SAVES = 2;
  private static final ActionRate RATE = new ActionRate(MIN_TICKS_BETWEEN_SAVES);

  private ControllerActions() {}

  public static void forget(ServerPlayerEntity player) {
    RATE.forget(player.getUuid());
  }

  public static void save(ServerPlayerEntity player, ControllerSetPayload action) {
    if (player.canModifyBlocks()
        && DeviceActions.canUse(player, action.pos())
        && player.getServerWorld().getBlockEntity(action.pos())
            instanceof ControllerBlockEntity controller
        && RATE.allow(player.getUuid(), player.getServer().getTicks())) {
      controller.setProgram(action.program());
    }
  }
}
