package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WireFaceTest {

  private static final BlockPos HOST = new BlockPos(3, 64, -7);

  @ParameterizedTest
  @EnumSource(Direction.class)
  void theOccupantIsTheBlockTheFaceLooksInto(Direction side) {
    assertEquals(HOST.offset(side), new WireFace(HOST, side).occupant());
  }
}
