package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.RadioBlockEntity;
import com.grahambartley.hardwired.device.ActionRate;
import com.grahambartley.hardwired.device.DeviceActions;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.network.RadioSetPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class RadioActions {

  private static final int MIN_TICKS_BETWEEN_PULSES = 4;
  private static final ActionRate PULSES = new ActionRate(MIN_TICKS_BETWEEN_PULSES);

  private RadioActions() {}

  public static void forget(ServerPlayerEntity player) {
    PULSES.forget(player.getUuid());
  }

  public static void set(ServerPlayerEntity player, RadioSetPayload action) {
    ServerWorld world = player.getServerWorld();
    BlockPos pos = action.pos();
    if (player.isSpectator()
        || !player.canInteractWithBlockAt(pos, DeviceActions.REACH_MARGIN)
        || !(world.getBlockEntity(pos) instanceof RadioBlockEntity radio)) {
      return;
    }
    radio.set(action.on(), action.frequency(), action.volume());
    if (PULSES.allow(player.getUuid(), player.getServer().getTicks())) {
      ElectronicsDriver.pulse(world, pos);
    } else {
      world.scheduleBlockTick(pos, radio.getCachedState().getBlock(), MIN_TICKS_BETWEEN_PULSES);
    }
    if (radio.isOn() && RadioTuner.best(world, pos, radio.frequency()).isPresent()) {
      ModCriteria.DEVICE.trigger(player, ModCriteria.TUNED_IN);
    }
  }
}
