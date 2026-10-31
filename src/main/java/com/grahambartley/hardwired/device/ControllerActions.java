package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.block.ControllerBlockEntity;
import com.grahambartley.hardwired.network.ControllerSetPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ControllerActions {

  private ControllerActions() {}

  public static void save(ServerPlayerEntity player, ControllerSetPayload action) {
    if (player.canModifyBlocks()
        && DeviceActions.canUse(player, action.pos())
        && player.getServerWorld().getBlockEntity(action.pos())
            instanceof ControllerBlockEntity controller) {
      controller.setProgram(action.program());
    }
  }
}
