package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.network.RadioSetPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class RadioActions {

  private static final double REACH_MARGIN = 1.0;

  private RadioActions() {}

  public static void set(ServerPlayerEntity player, RadioSetPayload action) {
    ServerWorld world = player.getServerWorld();
    BlockPos pos = action.pos();
    if (player.isSpectator()
        || !player.canInteractWithBlockAt(pos, REACH_MARGIN)
        || !(world.getBlockEntity(pos) instanceof RadioBlockEntity radio)) {
      return;
    }
    radio.set(action.on(), action.frequency(), action.volume());
    ElectronicsDriver.wake(world, pos);
    if (radio.isOn() && RadioTuner.best(world, pos, radio.frequency()).isPresent()) {
      ModCriteria.DEVICE.trigger(player, ModCriteria.TUNED_IN);
    }
  }
}
