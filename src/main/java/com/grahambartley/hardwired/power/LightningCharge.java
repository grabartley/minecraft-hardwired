package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.phone.Switchboard;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    WireEngine engine = WireEngine.of(world);
    Set<WireNetwork> struck = new HashSet<>(engine.power().networksAt(rod));
    struck.addAll(engine.grid().spanNetworksAt(rod.down()));
    Switchboard.of(world).crackle(engine.power().gridOf(struck));
  }
}
