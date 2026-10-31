package com.grahambartley.hardwired.wire;

import java.util.ArrayList;
import java.util.List;
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
    for (WireCell cell : carried) {
      engine.lay(
          new WireCell(
              new WireFace(cell.face().pos().offset(motion), cell.face().side()), cell.color()));
    }
  }
}
