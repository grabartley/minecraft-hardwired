package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.wire.TouchPoint;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ChargeNetwork {

  private ChargeNetwork() {}

  public static List<ChargeStore> storesFor(ServerWorld world, BlockPos pos) {
    WireEngine engine = WireEngine.of(world);
    return engine.grid().networksTouching(pos).stream()
        .flatMap(network -> network.touches().stream())
        .map(TouchPoint::pos)
        .distinct()
        .filter(engine::isLoaded)
        .sorted()
        .map(world::getBlockEntity)
        .filter(ChargeStore.class::isInstance)
        .map(ChargeStore.class::cast)
        .toList();
  }

  public static boolean tryDrain(ServerWorld world, BlockPos pos, int amount) {
    return amount == 0 || ChargeLedger.tryDrain(storesFor(world, pos), amount);
  }

  public static int fill(ServerWorld world, BlockPos pos, int amount) {
    return ChargeLedger.fill(storesFor(world, pos), amount);
  }
}
