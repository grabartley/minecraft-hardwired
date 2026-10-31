package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

class WirePathTest {

  private static WireFace floor(int x) {
    return new WireFace(new BlockPos(x, 64, 0), Direction.UP);
  }

  @Test
  void bridgesASkippedFace() {
    assertEquals(
        List.of(floor(1), floor(2)), WirePath.between(floor(0), floor(2), face -> true, 400));
  }

  @Test
  void fallsBackToTheTargetWhenNoRouteExists() {
    assertEquals(List.of(floor(5)), WirePath.between(floor(0), floor(5), face -> false, 400));
  }
}
