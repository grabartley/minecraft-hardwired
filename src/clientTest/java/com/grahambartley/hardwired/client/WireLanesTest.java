package com.grahambartley.hardwired.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireLanesTest {

  private static final BlockPos HOST = new BlockPos(3, 64, 3);
  private static final int EAST_WEST = CellView.armMask(Set.of(Direction.EAST, Direction.WEST));
  private static final int NORTH_SOUTH = CellView.armMask(Set.of(Direction.NORTH, Direction.SOUTH));

  private static CellView cell(int color, Direction side, int arms) {
    return new CellView(HOST.asLong(), side.getId(), color, 0, arms);
  }

  private static Vec3d onTop(double x, double z) {
    return new Vec3d(HOST.getX() + 0.5 + x, HOST.getY() + 1, HOST.getZ() + 0.5 + z);
  }

  @Test
  void aColourAloneOnAFaceStaysCentred() {
    assertArrayEquals(
        new double[] {0, 0},
        WireLanes.offsets(List.of(cell(1, Direction.UP, 0), cell(2, Direction.NORTH, 0))));
  }

  @ParameterizedTest(name = "{0} colours")
  @ValueSource(ints = {2, 3, 4, 8, 17})
  void everyColourOnAFaceGetsItsOwnOffsetInsideTheBlock(int colours) {
    double[] offsets =
        WireLanes.offsets(
            IntStream.range(0, colours).mapToObj(c -> cell(c, Direction.UP, 0)).toList());
    assertEquals(colours, Arrays.stream(offsets).distinct().count());
    assertTrue(Arrays.stream(offsets).allMatch(offset -> Math.abs(offset) < 0.45));
    assertEquals(0, Arrays.stream(offsets).sum(), 1.0E-9);
  }

  @Test
  void twoCellsOfOneColourShareALane() {
    double[] offsets =
        WireLanes.offsets(
            List.of(cell(1, Direction.UP, 0), cell(1, Direction.UP, 8), cell(2, Direction.UP, 0)));
    assertEquals(offsets[0], offsets[1]);
    assertNotEquals(offsets[0], offsets[2]);
  }

  @ParameterizedTest
  @EnumSource(Direction.class)
  void aShiftStaysInThePlaneOfTheFace(Direction side) {
    Vec3d shift = WireLanes.shift(side, 0.1);
    assertEquals(0, shift.dotProduct(Vec3d.of(side.getVector())), 1.0E-9);
    assertEquals(0.2, Math.abs(shift.x) + Math.abs(shift.y) + Math.abs(shift.z), 1.0E-9);
  }

  @ParameterizedTest(name = "hit {0} {1} picks colour {2}")
  @CsvSource({
    "-0.05, -0.3, 1",
    "0.05, -0.3, 2",
    "0.3, -0.05, 1",
    "0.3, 0.05, 2",
  })
  void theCellNearestTheHitWinsAlongEitherLine(double x, double z, int expected) {
    List<CellView> sharing =
        List.of(
            cell(1, Direction.UP, NORTH_SOUTH | EAST_WEST),
            cell(2, Direction.UP, NORTH_SOUTH | EAST_WEST));
    assertEquals(expected, WireLanes.nearest(sharing, onTop(x, z)).orElseThrow().color());
  }

  @ParameterizedTest(name = "hit {0} {1} picks colour {2}")
  @CsvSource({"0.3, -0.05, 1", "-0.05, 0.3, 2"})
  void theCellWhoseLineRunsThroughTheHitBeatsACloserCentre(double x, double z, int expected) {
    List<CellView> crossing =
        List.of(cell(1, Direction.UP, EAST_WEST), cell(2, Direction.UP, NORTH_SOUTH));
    assertEquals(expected, WireLanes.nearest(crossing, onTop(x, z)).orElseThrow().color());
  }

  @Test
  void aHitOnABlockInFrontOfTheFaceStillPicksByPosition() {
    List<CellView> sharing = List.of(cell(1, Direction.UP, 0), cell(2, Direction.UP, 0));
    assertEquals(2, WireLanes.nearest(sharing, onTop(0.1, 0.1).add(0, 1, 0)).orElseThrow().color());
  }

  @Test
  void noCellsGiveNothing() {
    assertEquals(Optional.empty(), WireLanes.nearest(List.of(), onTop(0, 0)));
  }
}
