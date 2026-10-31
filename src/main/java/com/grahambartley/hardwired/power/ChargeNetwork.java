package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class ChargeNetwork {

  private ChargeNetwork() {}

  public static List<ChargeStore> storesFor(ServerWorld world, BlockPos pos) {
    return WireEngine.of(world).allOnNetwork(pos, ChargeStore.class);
  }

  public static boolean tryDrain(ServerWorld world, BlockPos pos, int amount) {
    return amount == 0 || ChargeLedger.tryDrain(storesFor(world, pos), amount);
  }

  public static int fill(ServerWorld world, BlockPos pos, int amount) {
    return ChargeLedger.fill(storesFor(world, pos), amount);
  }
}
