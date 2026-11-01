package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DeskShapesTest {

  private static final double FULL = 16;

  @ParameterizedTest
  @EnumSource(DeskPart.class)
  void everyBoxStaysInsideItsCellOnEveryFacing(DeskPart part) {
    for (Direction facing : Direction.Type.HORIZONTAL) {
      for (double[] box : DeskShapes.boxes(part, facing)) {
        assertTrue(Arrays.stream(box).allMatch(value -> value >= 0 && value <= FULL));
        assertTrue(box[0] < box[3] && box[1] < box[4] && box[2] < box[5]);
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    "EAST, 0, 6, 10.75, 16, 16, 13",
    "SOUTH, 3, 6, 0, 5.25, 16, 16",
    "WEST, 0, 6, 3, 16, 16, 5.25",
    "NORTH, 10.75, 6, 0, 13, 16, 16"
  })
  void theSlateTurnsWithTheDesk(
      Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
    double[] slate = DeskShapes.boxes(DeskPart.PANEL, facing)[3];
    assertArrayEquals(new double[] {x0, y0, z0, x1, y1, z1}, slate);
  }

  @ParameterizedTest
  @EnumSource(DeskPart.class)
  void aNonEmptyShapeExistsForEveryFacing(DeskPart part) {
    for (Direction facing : Direction.Type.HORIZONTAL) {
      assertEquals(false, DeskShapes.shape(part, facing).isEmpty());
    }
  }
}
