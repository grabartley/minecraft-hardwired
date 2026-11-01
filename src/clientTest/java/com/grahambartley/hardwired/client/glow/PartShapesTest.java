package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PartShapesTest {

  private static final VoxelShape CARPET = VoxelShapes.cuboid(0, 0, 0, 1, 0.0625, 1);
  private static final VoxelShape TALL = VoxelShapes.cuboid(0, 0, 0, 1, 1.5, 1);

  static Stream<Arguments> shapes() {
    return Stream.of(
        Arguments.of("carpet", CARPET, CARPET, true),
        Arguments.of("plant with no collision", CARPET, VoxelShapes.empty(), false),
        Arguments.of("fence taller than its outline", CARPET, TALL, false),
        Arguments.of("nothing", VoxelShapes.empty(), VoxelShapes.empty(), false));
  }

  @ParameterizedTest(name = "{0}: {3}")
  @MethodSource("shapes")
  void onlySolidMatchingShapesCount(
      String name, VoxelShape outline, VoxelShape collision, boolean expected) {
    assertEquals(expected, PartShapes.sameShape(outline, collision));
  }
}
