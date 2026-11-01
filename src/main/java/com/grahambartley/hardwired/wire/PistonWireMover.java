package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class PistonWireMover {

  private PistonWireMover() {}

  public static void move(
      ServerWorld world, List<BlockPos> broken, List<BlockPos> moved, Direction motion) {
    WireEngine engine = WireEngine.of(world);
    broken.forEach(engine::dropWireOn);
    List<WireCell> carried = new ArrayList<>();
    for (BlockPos pos : moved) {
      carried.addAll(engine.grid().cellsOn(pos));
    }
    carried.forEach(engine::cut);
    int limit = Hardwired.SERVER_CONFIG.wire().maxNetworkSize();
    land(engine.grid(), carried, motion, limit, engine::lay).forEach(engine::dropItem);
  }

  static List<WireCell> land(
      WireGrid grid, List<WireCell> carried, Direction motion, int limit, Consumer<WireCell> lay) {
    List<WireCell> refused = new ArrayList<>();
    for (WireCell cell : carried) {
      WireCell landed =
          cell.onFace(new WireFace(cell.face().pos().offset(motion), cell.face().side()));
      if (grid.contains(landed) || grid.sizeAfterJoining(landed) > limit) {
        refused.add(landed);
      } else {
        lay.accept(landed);
      }
    }
    return refused;
  }
}
