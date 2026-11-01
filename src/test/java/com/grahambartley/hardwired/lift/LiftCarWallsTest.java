package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class LiftCarWallsTest {

  private static boolean walled(List<Box> walls, Vec3d point) {
    return walls.stream().anyMatch(wall -> wall.contains(point));
  }

  @ParameterizedTest(name = "gate facing {0}, span {1}")
  @CsvSource({"NORTH,1", "SOUTH,1", "EAST,1", "WEST,1", "NORTH,2", "SOUTH,2", "EAST,2", "WEST,2"})
  void onlyTheSideFacingTheGateIsOpen(Direction gateFacing, int span) {
    int turns = LiftCarTurn.quarterTurns(gateFacing);
    List<Box> walls = LiftCarWalls.boxes(0, 0, 0, span, turns, false);
    double edge = span / 2.0 - 0.01;
    for (Direction side : Direction.Type.HORIZONTAL) {
      Vec3d point = new Vec3d(side.getOffsetX() * edge, 1, side.getOffsetZ() * edge);
      assertEquals(side != gateFacing.getOpposite(), walled(walls, point), side.toString());
    }
  }

  @ParameterizedTest(name = "gate facing {0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void theOpeningLooksAtTheGate(Direction gateFacing) {
    assertEquals(
        gateFacing.getOpposite(), LiftCarWalls.opening(LiftCarTurn.quarterTurns(gateFacing)));
  }

  @ParameterizedTest(name = "span {0}")
  @CsvSource({"1", "2"})
  void aPlayerFitsInsideUnderTheRoof(int span) {
    List<Box> walls = LiftCarWalls.boxes(0, 0, 0, span, 0, false);
    Box player = new Box(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
    assertFalse(walls.stream().anyMatch(player::intersects));
    assertTrue(walled(walls, new Vec3d(0, 1.9, 0)), "the roof is solid");
  }

  @ParameterizedTest(name = "feet at {0} rest on {1}")
  @CsvSource({"10,10", "12,10", "9.98,9.98", "9.6,9.6", "9.4,10"})
  void theFloorNeverRisesPastFeetThatSankALittle(double feet, double top) {
    assertEquals(top, LiftCarWalls.floor(0, 10, 0, 1, feet).maxY, 1.0E-9);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void aSealedCarHasNoOpeningAtAll(Direction side) {
    List<Box> walls = LiftCarWalls.boxes(0, 0, 0, 1, 0, true);
    assertTrue(
        walled(walls, new Vec3d(side.getOffsetX() * 0.49, 1, side.getOffsetZ() * 0.49)),
        side.toString());
  }
}
