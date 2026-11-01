package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StreetlightShapesTest {

  private static final double FULL = 16;
  private static final double EPSILON = 1E-6;

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void everyPartReachesItsNeighbours(int part) {
    assertEquals(0, StreetlightShapes.shape(part).getMin(Direction.Axis.Y), EPSILON);
    assertEquals(1, StreetlightShapes.shape(part).getMax(Direction.Axis.Y), EPSILON);
  }

  @ParameterizedTest
  @CsvSource({"0, 2, 14", "1, 5.5, 10.5", "2, 3, 13", "3, 3, 13"})
  void aPartIsAsWideAsItsModel(int part, double min, double max) {
    assertEquals(min / FULL, StreetlightShapes.shape(part).getMin(Direction.Axis.X), EPSILON);
    assertEquals(max / FULL, StreetlightShapes.shape(part).getMax(Direction.Axis.X), EPSILON);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void everyBoxIsInsideTheCell(int part) {
    for (double[] box : StreetlightShapes.boxes(part)) {
      assertTrue(box[0] >= 0 && box[1] >= 0 && box[2] >= 0);
      assertTrue(box[3] <= FULL && box[4] <= FULL && box[5] <= FULL);
    }
  }
}
