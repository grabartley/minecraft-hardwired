package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireNetwork;
import java.util.Comparator;
import java.util.List;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

final class TestPower {

  private TestPower() {}

  static void keepFed(TestContext context) {
    context.runAtEveryTick(() -> feed(context));
  }

  private static void feed(TestContext context) {
    WireEngine engine = WireEngine.of(context.getWorld());
    Box bounds = context.getTestBox().expand(2);
    for (WireNetwork network : List.copyOf(engine.grid().networks())) {
      if (!network.gridState().givesPower()
          && network.lastSources().isEmpty()
          && inBounds(network, bounds)) {
        network.cells().stream()
            .map(cell -> cell.face().pos())
            .filter(host -> !isDevice(context, host))
            .min(Comparator.comparingInt(BlockPos::getY).thenComparingLong(BlockPos::asLong))
            .ifPresent(host -> feedAt(context, host));
      }
    }
  }

  private static boolean isDevice(TestContext context, BlockPos host) {
    return context.getWorld().getBlockState(host).getBlock() instanceof WireDevice;
  }

  private static boolean inBounds(WireNetwork network, Box bounds) {
    for (WireCell cell : network.cells()) {
      if (bounds.contains(cell.face().pos().toCenterPos())) {
        return true;
      }
    }
    return false;
  }

  private static void feedAt(TestContext context, BlockPos host) {
    context.getWorld().setBlockState(host, ModBlocks.LODESTONE_DYNAMO.getDefaultState());
  }
}
