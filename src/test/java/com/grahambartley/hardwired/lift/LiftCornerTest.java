package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class LiftCornerTest {

  private static final BlockPos HEAD = new BlockPos(10, 64, -20);

  @ParameterizedTest(name = "{0}")
  @EnumSource(LiftCorner.class)
  void everyCellResolvesBackToTheHead(LiftCorner corner) {
    assertEquals(HEAD, corner.head(corner.cell(HEAD)));
    assertEquals(corner, LiftCorner.of(HEAD, corner.cell(HEAD)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void aPlacementSpansFourCellsFromThePlacedOne(Direction look) {
    BlockPos head = LiftCorner.headFor(HEAD, look);
    assertEquals(4, new HashSet<>(LiftCorner.cells(head)).size());
    assertTrue(LiftCorner.cells(head).contains(HEAD));
    assertTrue(LiftCorner.cells(head).contains(HEAD.offset(look)));
    assertTrue(LiftCorner.cells(head).contains(HEAD.offset(look.rotateYClockwise())));
  }

  @ParameterizedTest(name = "{0} with front {1} faces {2}")
  @CsvSource({
    "NORTH_WEST,NORTH,NORTH",
    "NORTH_EAST,NORTH,NORTH",
    "SOUTH_WEST,NORTH,SOUTH",
    "SOUTH_EAST,NORTH,SOUTH",
    "NORTH_EAST,EAST,EAST",
    "NORTH_WEST,EAST,WEST"
  })
  void eachRowFacesOutward(LiftCorner corner, Direction front, Direction facing) {
    assertEquals(facing, corner.facing(front));
  }
}
