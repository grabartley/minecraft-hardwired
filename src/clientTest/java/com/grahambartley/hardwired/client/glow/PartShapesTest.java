package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.block.BlockRenderType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartShapesTest {

  @ParameterizedTest(name = "{0} with block entity {1}: {2}")
  @CsvSource({
    "MODEL,false,true",
    "MODEL,true,false",
    "ENTITYBLOCK_ANIMATED,true,false",
    "ENTITYBLOCK_ANIMATED,false,false",
    "INVISIBLE,false,false"
  })
  void onlyPlainModelsTakeTint(BlockRenderType type, boolean entity, boolean expected) {
    assertEquals(expected, PartShapes.isPlainModel(type, entity));
  }

  @ParameterizedTest(name = "lower step, {0} reaches the edge: {1}")
  @CsvSource({"UP,false", "DOWN,true", "NORTH,true", "SOUTH,true", "EAST,true", "WEST,true"})
  void onlyFacesOnTheCellBoundaryReachIt(Direction side, boolean expected) {
    Box lowerStep = new Box(0, 0, 0, 1, 0.5, 1);
    assertEquals(expected, PartShapes.reachesCellEdge(lowerStep, side));
  }
}
