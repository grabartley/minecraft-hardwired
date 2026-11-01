package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PistonWireMoverTest {

  private static WireCell floor(int x, Hue hue) {
    return new WireCell(new WireFace(new BlockPos(x, 64, 0), Direction.UP), hue);
  }

  private static void run(WireGrid grid, int from, int to) {
    for (int x = from; x <= to; x++) {
      grid.add(floor(x, Hue.PLAIN));
    }
  }

  private static List<WireCell> carriedRow(int length) {
    List<WireCell> carried = new ArrayList<>();
    for (int x = 0; x < length; x++) {
      carried.add(new WireCell(new WireFace(new BlockPos(x, 64, 5), Direction.UP), Hue.PLAIN));
    }
    return carried;
  }

  @ParameterizedTest(name = "carrying {0} onto a run of {1} with limit {2} refuses {3}")
  @CsvSource({"3, 4, 100, 0", "3, 4, 7, 0", "3, 4, 6, 1", "3, 4, 5, 1", "3, 4, 4, 1"})
  void landingStopsAtTheLimitAndEveryCellIsLaidOrRefused(
      int carrying, int run, int limit, int expectedRefused) {
    WireGrid grid = new WireGrid();
    for (int x = 0; x < run; x++) {
      grid.add(new WireCell(new WireFace(new BlockPos(x - run, 64, 6), Direction.UP), Hue.PLAIN));
    }
    List<WireCell> carried = carriedRow(carrying);
    List<WireCell> refused = PistonWireMover.land(grid, carried, Direction.SOUTH, limit, grid::add);
    assertEquals(expectedRefused, refused.size());
    assertEquals(
        run + carrying - expectedRefused,
        grid.networks().stream().mapToInt(WireNetwork::size).sum());
    assertTrue(grid.networks().stream().allMatch(network -> network.size() <= limit));
  }

  @Test
  void aCellThatWouldBridgeTwoFullNetworksIsRefused() {
    WireGrid grid = new WireGrid();
    run(grid, 0, 2);
    run(grid, 4, 6);
    WireCell bridge = new WireCell(new WireFace(new BlockPos(3, 64, -1), Direction.UP), Hue.PLAIN);
    List<WireCell> refused =
        PistonWireMover.land(grid, List.of(bridge), Direction.SOUTH, 6, grid::add);
    assertEquals(List.of(floor(3, Hue.PLAIN)), refused);
    assertEquals(2, grid.networks().size());
  }

  @ParameterizedTest(name = "direction {0}")
  @CsvSource({"EAST, 1, 64, 0", "UP, 0, 65, 0", "NORTH, 0, 64, -1"})
  void aLandedCellKeepsItsColourSideAndOwner(Direction motion, int x, int y, int z) {
    WireGrid grid = new WireGrid();
    UUID owner = new UUID(1, 1);
    WireCell cell =
        new WireCell(new WireFace(new BlockPos(0, 64, 0), Direction.UP), Hue.RED, owner);
    PistonWireMover.land(grid, List.of(cell), motion, 10, grid::add);
    assertTrue(
        grid.contains(
            new WireCell(new WireFace(new BlockPos(x, y, z), Direction.UP), Hue.RED, owner)));
  }

  @Test
  void landingOnAnIdenticalCellIsRefusedSoNoItemVanishes() {
    WireGrid grid = new WireGrid();
    WireCell already = new WireCell(new WireFace(new BlockPos(1, 64, 0), Direction.UP), Hue.PLAIN);
    grid.add(already);
    assertEquals(
        List.of(already),
        PistonWireMover.land(grid, List.of(floor(0, Hue.PLAIN)), Direction.EAST, 10, grid::add));
  }
}
