package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WireGridTest {

  private static WireCell floor(int x, Hue hue) {
    return new WireCell(new WireFace(new BlockPos(x, 64, 0), Direction.UP), hue);
  }

  private static WireGrid line(int length) {
    WireGrid grid = new WireGrid();
    IntStream.range(0, length).forEach(x -> grid.add(floor(x, Hue.PLAIN)));
    return grid;
  }

  @Test
  void touchingFacesOfOneColourJoin() {
    WireGrid grid = line(3);
    assertEquals(1, grid.networks().size());
    assertEquals(3, grid.networkOf(floor(0, Hue.PLAIN)).size());
  }

  @Test
  void differentColoursOnOneFaceNeverJoin() {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.add(floor(0, Hue.RED));
    assertEquals(2, grid.networks().size());
  }

  @Test
  void bridgingTwoLinesMergesThem() {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.add(floor(2, Hue.PLAIN));
    grid.networkOf(floor(2, Hue.PLAIN)).setOn(true);
    grid.add(floor(1, Hue.PLAIN));
    assertEquals(1, grid.networks().size());
    assertTrue(grid.networkOf(floor(0, Hue.PLAIN)).isOn());
  }

  @ParameterizedTest(name = "cutting face {0} of 5")
  @ValueSource(ints = {1, 2, 3})
  void cuttingAMiddleFaceSplitsTheNetwork(int cut) {
    WireGrid grid = line(5);
    grid.remove(floor(cut, Hue.PLAIN));
    assertEquals(2, grid.networks().size());
    assertNotSame(grid.networkOf(floor(0, Hue.PLAIN)), grid.networkOf(floor(4, Hue.PLAIN)));
    assertEquals(4, grid.networks().stream().mapToInt(WireNetwork::size).sum());
  }

  @Test
  void cuttingAnEndKeepsOneNetwork() {
    WireGrid grid = line(3);
    grid.remove(floor(2, Hue.PLAIN));
    assertEquals(1, grid.networks().size());
  }

  @Test
  void aSplitPartKeepsNoStaleTouches() {
    WireGrid grid = line(3);
    WireNetwork network = grid.networkOf(floor(0, Hue.PLAIN));
    grid.remove(floor(1, Hue.PLAIN));
    assertTrue(grid.networksTouching(new BlockPos(2, 64, 0)).stream().noneMatch(network::equals));
  }

  @Test
  void removingEverythingLeavesNothingTouched() {
    WireGrid grid = line(4);
    IntStream.range(0, 4).forEach(x -> grid.remove(floor(x, Hue.PLAIN)));
    assertTrue(grid.networks().isEmpty());
    assertFalse(grid.isTouched());
  }

  @Test
  void wireChangeClearsATrip() {
    WireGrid grid = line(2);
    WireNetwork network = grid.networkOf(floor(0, Hue.PLAIN));
    network.setTripped(true);
    grid.add(floor(2, Hue.PLAIN));
    assertFalse(network.isTripped());
  }

  @Test
  void layingTheSameCellTwiceCountsOnce() {
    WireGrid grid = new WireGrid();
    WireNetwork first = grid.add(floor(0, Hue.PLAIN));
    assertSame(first, grid.add(floor(0, Hue.PLAIN)));
    assertEquals(1, first.size());
  }

  @Test
  void sizeAfterAddingCountsEveryNetworkItWouldJoin() {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.add(floor(2, Hue.PLAIN));
    assertEquals(3, grid.sizeAfterAdding(floor(1, Hue.PLAIN)));
  }

  @Test
  void changesBumpTheChunkVersion() {
    WireGrid grid = new WireGrid();
    long chunk = ChunkPos.toLong(0, 0);
    int before = grid.chunkVersion(chunk);
    grid.add(floor(0, Hue.PLAIN));
    assertTrue(grid.chunkVersion(chunk) > before);
  }

  @Test
  void poweredFollowsTheNetworkAtATouchedPosition() {
    WireGrid grid = line(2);
    long occupant = new BlockPos(0, 65, 0).asLong();
    assertFalse(grid.isPowered(occupant));
    grid.networkOf(floor(0, Hue.PLAIN)).setOn(true);
    assertTrue(grid.isPowered(occupant));
  }
}
