package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireViewer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class SourceSwitches {

  private SourceSwitches() {}

  public static boolean mayWork(ServerPlayerEntity player, BlockPos pos) {
    WireViewer viewer = WireViewer.of(player);
    boolean allowed =
        WireEngine.of(player.getServerWorld()).power().networksAt(pos).stream()
            .allMatch(viewer::canSee);
    if (!allowed) {
      player.sendMessage(Text.translatable("message.hardwired.switch_sealed"), true);
    }
    return allowed;
  }
}
