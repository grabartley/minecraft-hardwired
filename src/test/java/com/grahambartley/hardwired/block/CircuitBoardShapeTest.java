package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CircuitBoardShapeTest {

  private static final double PIXEL = 1.0 / 16;

  @ParameterizedTest(name = "{0} rear wall stands at {1},{2}, deck at {3},{4}")
  @CsvSource({"NORTH,8,12,8,4", "SOUTH,8,4,8,12", "EAST,4,8,12,8", "WEST,12,8,4,8"})
  void onlyTheRearOfTheBenchIsTwelveHigh(
      Direction facing, double rearX, double rearZ, double frontX, double frontZ) {
    VoxelShape shape = CircuitBoardShape.of(facing);
    assertEquals(12 * PIXEL, top(shape, rearX, rearZ), 1e-6);
    assertEquals(9 * PIXEL, top(shape, frontX, frontZ), 1e-6);
  }

  private static double top(VoxelShape shape, double x, double z) {
    return shape.getBoundingBoxes().stream()
        .filter(box -> box.contains(x * PIXEL, box.minY + PIXEL, z * PIXEL))
        .mapToDouble(box -> box.maxY)
        .max()
        .orElse(0);
  }
}
