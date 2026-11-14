package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class LightningCharge {

  private LightningCharge() {}

  public static void struck(ServerWorld world, BlockPos rod) {
    int charge = Hardwired.SERVER_CONFIG.power().lightningCharge();
    List<ChargeStore> cells = new ArrayList<>(ChargeNetwork.cellsFor(world, rod));
    ChargeNetwork.cellsOnSpansAt(world, rod.down()).stream()
        .filter(cell -> !cells.contains(cell))
        .forEach(cells::add);
    if (ChargeLedger.fill(cells, charge) > 0) {
      ModCriteria.DEVICE.triggerNear(world, rod, ModCriteria.LIGHTNING_CHARGED);
    }
  }
}
