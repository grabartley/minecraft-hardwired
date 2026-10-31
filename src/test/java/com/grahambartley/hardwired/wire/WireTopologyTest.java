package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class WireTopologyTest {

  private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

  @ParameterizedTest(name = "a face on {0} has twelve neighbours")
  @EnumSource(Direction.class)
  void everyFaceHasTwelveDistinctNeighbours(Direction side) {
    List<WireFace> neighbours = WireTopology.neighbours(new WireFace(ORIGIN, side));
    assertEquals(12, new HashSet<>(neighbours).size());
  }

  @ParameterizedTest(name = "touching is symmetric from {0}")
  @EnumSource(Direction.class)
  void touchingIsSymmetric(Direction side) {
    WireFace face = new WireFace(ORIGIN, side);
    for (WireFace neighbour : WireTopology.neighbours(face)) {
      assertTrue(WireTopology.neighbours(neighbour).contains(face), () -> neighbour.toString());
    }
  }

  static Stream<WireFace[]> cornerCases() {
    WireFace floor = new WireFace(ORIGIN, Direction.UP);
    return Stream.of(
        new WireFace[] {floor, new WireFace(ORIGIN.east(), Direction.UP)},
        new WireFace[] {floor, new WireFace(ORIGIN, Direction.EAST)},
        new WireFace[] {floor, new WireFace(ORIGIN.up().east(), Direction.WEST)});
  }

  @ParameterizedTest(name = "{0} joins {1}")
  @MethodSource("cornerCases")
  void flatOuterAndInnerCornersJoin(WireFace from, WireFace to) {
    assertTrue(WireTopology.neighbours(from).contains(to));
  }

  @org.junit.jupiter.api.Test
  void oppositeSidesOfOneBlockDoNotTouch() {
    assertTrue(
        !WireTopology.neighbours(new WireFace(ORIGIN, Direction.UP))
            .contains(new WireFace(ORIGIN, Direction.DOWN)));
  }

  @org.junit.jupiter.api.Test
  void aRunningWireOnlyTouchesItsHostAndOccupant() {
    Set<Direction> through = EnumSet.of(Direction.EAST, Direction.WEST);
    assertEquals(2, WireTopology.touches(new WireFace(ORIGIN, Direction.UP), through).size());
  }

  @org.junit.jupiter.api.Test
  void aDeadEndTouchesEverySideExceptBackAlongItself() {
    List<TouchPoint> touches =
        WireTopology.touches(new WireFace(ORIGIN, Direction.UP), EnumSet.of(Direction.WEST));
    assertTrue(touches.contains(new TouchPoint(ORIGIN.up().east(), Direction.EAST)));
    assertTrue(touches.contains(new TouchPoint(ORIGIN.up().north(), Direction.NORTH)));
    assertTrue(touches.contains(new TouchPoint(ORIGIN.up().south(), Direction.SOUTH)));
    assertEquals(5, touches.size());
  }

  @org.junit.jupiter.api.Test
  void aLoneFaceTouchesAllFourSides() {
    assertEquals(
        6,
        WireTopology.touches(new WireFace(ORIGIN, Direction.UP), EnumSet.noneOf(Direction.class))
            .size());
  }
}
