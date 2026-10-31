package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LiftFloorShapesTest {

  private static final double PLAYER_WIDTH = 0.6;
  private static final double FULL_HEIGHT = 1.0;

  private static boolean blocks(VoxelShape shape, Box box) {
    return shape.getBoundingBoxes().stream().anyMatch(box::intersects);
  }

  private static Box playerWalkingThrough(Direction side) {
    double half = PLAYER_WIDTH / 2;
    return side.getAxis() == Direction.Axis.Z
        ? new Box(0.5 - half, 0, 0, 0.5 + half, 1, 1)
        : new Box(0, 0, 0.5 - half, 1, 1, 0.5 + half);
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "EAST", "SOUTH", "WEST"})
  void aClosedGateWallsOffTheWholeOpeningOnTheShaftSide(Direction side) {
    VoxelShape closed = LiftFloorShapes.closed(side);
    assertTrue(blocks(closed, playerWalkingThrough(side)));
    assertEquals(FULL_HEIGHT, closed.getMax(Direction.Axis.Y));
    double reach = side.getDirection() == Direction.AxisDirection.POSITIVE ? 0.9 : 0.1;
    assertTrue(
        closed
            .getBoundingBox()
            .contains(
                side.getAxis() == Direction.Axis.X ? reach : 0.5,
                0.5,
                side.getAxis() == Direction.Axis.Z ? reach : 0.5));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "EAST", "SOUTH", "WEST"})
  void anOpenGateLeavesRoomToWalkThrough(Direction side) {
    assertFalse(blocks(LiftFloorShapes.open(side), playerWalkingThrough(side)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "EAST", "SOUTH", "WEST"})
  void aPlayerStandingInTheGateCellIsNotInsideAClosedGate(Direction side) {
    Box standing = new Box(0.2, 0, 0.2, 0.8, 1, 0.8);
    assertFalse(blocks(LiftFloorShapes.closed(side), standing));
  }
}
