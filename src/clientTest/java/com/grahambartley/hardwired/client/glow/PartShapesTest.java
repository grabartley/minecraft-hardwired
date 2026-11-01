package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import net.minecraft.block.BlockRenderType;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PartShapesTest {

  private static final VoxelShape CARPET = VoxelShapes.cuboid(0, 0, 0, 1, 0.0625, 1);

  static Stream<Arguments> blocks() {
    return Stream.of(
        Arguments.of("carpet", BlockRenderType.MODEL, false, CARPET, true),
        Arguments.of("plant", BlockRenderType.MODEL, false, VoxelShapes.empty(), false),
        Arguments.of("device", BlockRenderType.MODEL, true, CARPET, false),
        Arguments.of("chest", BlockRenderType.ENTITYBLOCK_ANIMATED, true, CARPET, false),
        Arguments.of("barrier", BlockRenderType.INVISIBLE, false, CARPET, false));
  }

  @ParameterizedTest(name = "{0}: {4}")
  @MethodSource("blocks")
  void onlyPlainSolidModelsTakeTint(
      String name, BlockRenderType type, boolean entity, VoxelShape collision, boolean expected) {
    assertEquals(expected, PartShapes.takesTint(type, entity, collision));
  }
}
