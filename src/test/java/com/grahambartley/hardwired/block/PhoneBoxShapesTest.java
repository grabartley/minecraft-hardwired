package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PhoneBoxShapesTest {

  private static final double HALF_WIDTH = 0.3;
  private static final double HEIGHT = 1.8;
  private static final double FLOOR = 1 / 16.0;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> facingsAndDoors() {
    return Stream.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
        .flatMap(facing -> Stream.of(Arguments.of(facing, false), Arguments.of(facing, true)));
  }

  private static VoxelShape standingRoom(Direction facing, boolean open) {
    return VoxelShapes.union(
        PhoneBoxShapes.shape(PhoneBoxPart.LOWER, facing, open),
        PhoneBoxShapes.shape(PhoneBoxPart.MIDDLE, facing, open).offset(0, 1, 0));
  }

  private static boolean hits(VoxelShape shape, Box player) {
    return shape.getBoundingBoxes().stream().anyMatch(player::intersects);
  }

  private static Box playerAt(double x, double z) {
    return new Box(
        x - HALF_WIDTH, FLOOR, z - HALF_WIDTH, x + HALF_WIDTH, FLOOR + HEIGHT, z + HALF_WIDTH);
  }

  @ParameterizedTest(name = "{0} open {1}")
  @MethodSource("facingsAndDoors")
  void aPlayerStandsInsideWithoutTouchingAWall(Direction facing, boolean open) {
    assertEquals(false, hits(standingRoom(facing, open), playerAt(0.5, 0.5)));
  }

  @ParameterizedTest(name = "{0} open {1}")
  @MethodSource("facingsAndDoors")
  void onlyAnOpenDoorLetsAPlayerThroughTheDoorway(Direction facing, boolean open) {
    double x = 0.5 + facing.getOffsetX() * 0.5;
    double z = 0.5 + facing.getOffsetZ() * 0.5;
    assertEquals(!open, hits(standingRoom(facing, open), playerAt(x, z)));
  }

  @ParameterizedTest(name = "{0} open {1}")
  @MethodSource("facingsAndDoors")
  void theBackWallAlwaysStopsAPlayer(Direction facing, boolean open) {
    double x = 0.5 - facing.getOffsetX() * 0.5;
    double z = 0.5 - facing.getOffsetZ() * 0.5;
    assertEquals(true, hits(standingRoom(facing, open), playerAt(x, z)));
  }

  @ParameterizedTest(name = "{0} at {1},{2} is {3} from the door")
  @CsvSource({"NORTH,0.5,0.1,0.1", "SOUTH,0.5,0.1,0.9", "EAST,0.9,0.5,0.1", "WEST,0.9,0.5,0.9"})
  void depthIsMeasuredFromTheDoor(Direction facing, double x, double z, double depth) {
    assertEquals(depth, PhoneBoxShapes.depthFromDoor(x, z, facing), 1e-9);
  }
}
