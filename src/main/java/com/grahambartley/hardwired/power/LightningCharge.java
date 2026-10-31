package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class LightningCharge {

  private LightningCharge() {}

  public static void struck(ServerWorld world, BlockPos rod) {
    int charge = Hardwired.SERVER_CONFIG.power().lightningCharge();
    if (ChargeNetwork.fill(world, rod, charge) > 0) {
      ModCriteria.DEVICE.triggerNear(world, rod, ModCriteria.LIGHTNING_CHARGED);
    }
  }
}
