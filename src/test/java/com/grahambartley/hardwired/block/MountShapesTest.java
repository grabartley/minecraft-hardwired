package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.stream.Stream;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MountShapesTest {

  private static final double[] VANILLA_LEVER_FLOOR = {5, 0, 4, 11, 6, 12};

  static Stream<Arguments> vanillaLeverBoxes() {
    return Stream.of(
        Arguments.of(BlockFace.WALL, Direction.NORTH, new double[] {5, 4, 10, 11, 12, 16}),
        Arguments.of(BlockFace.WALL, Direction.SOUTH, new double[] {5, 4, 0, 11, 12, 6}),
        Arguments.of(BlockFace.WALL, Direction.EAST, new double[] {0, 4, 5, 6, 12, 11}),
        Arguments.of(BlockFace.WALL, Direction.WEST, new double[] {10, 4, 5, 16, 12, 11}),
        Arguments.of(BlockFace.FLOOR, Direction.NORTH, new double[] {5, 0, 4, 11, 6, 12}),
        Arguments.of(BlockFace.FLOOR, Direction.EAST, new double[] {4, 0, 5, 12, 6, 11}),
        Arguments.of(BlockFace.CEILING, Direction.NORTH, new double[] {5, 10, 4, 11, 16, 12}),
        Arguments.of(BlockFace.CEILING, Direction.EAST, new double[] {4, 10, 5, 12, 16, 11}));
  }

  @ParameterizedTest(name = "{0} {1} matches the vanilla lever")
  @MethodSource("vanillaLeverBoxes")
  void mountsLikeVanillaLevers(BlockFace face, Direction facing, double[] expected) {
    assertArrayEquals(expected, MountShapes.mount(VANILLA_LEVER_FLOOR, face, facing));
  }
}
