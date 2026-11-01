package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.power.GridState;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  void layingAndCuttingReportEveryChangedTouch() {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.drainTouchChanges();
    grid.remove(floor(0, Hue.PLAIN));
    assertTrue(
        java.util.Arrays.stream(grid.drainTouchChanges())
            .anyMatch(pos -> pos == new BlockPos(0, 65, 0).asLong()));
    assertEquals(0, grid.drainTouchChanges().length);
  }

  @Test
  void liveNeedsTheNetworkOnAndItsGridGivingPower() {
    WireGrid grid = line(2);
    long occupant = new BlockPos(0, 65, 0).asLong();
    WireNetwork network = grid.networkOf(floor(0, Hue.PLAIN));
    assertFalse(grid.isOn(occupant));
    network.setOn(true);
    assertTrue(grid.isOn(occupant));
    assertFalse(grid.isLive(occupant));
    assertFalse(grid.hasPower(occupant));
    network.setGridState(GridState.LIVE);
    assertTrue(grid.isLive(occupant));
    assertTrue(grid.hasPower(occupant));
    network.setOn(false);
    assertFalse(grid.isLive(occupant));
    assertTrue(grid.hasPower(occupant));
  }

  @Test
  void onlyOpenWireOrOneOwnersSealsSharePower() {
    WireNetwork open = new WireNetwork(Hue.PLAIN, null);
    WireNetwork alice = new WireNetwork(Hue.PLAIN, ALICE);
    WireNetwork aliceToo = new WireNetwork(Hue.BLUE, ALICE);
    assertTrue(open.sharesPowerWith(new WireNetwork(Hue.BLUE, null)));
    assertTrue(alice.sharesPowerWith(aliceToo));
    assertFalse(open.sharesPowerWith(alice));
    assertFalse(alice.sharesPowerWith(new WireNetwork(Hue.PLAIN, new UUID(2, 2))));
  }

  private static final UUID ALICE = new UUID(1, 1);
  private static final UUID BOB = new UUID(2, 2);

  private static WireCell sealed(int x, UUID owner) {
    return floor(x, Hue.PLAIN).ownedBy(owner);
  }

  @ParameterizedTest(name = "{0} beside {1}")
  @MethodSource("differentOwners")
  void wireOfDifferentOwnersNeverJoins(UUID first, UUID second) {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN).ownedBy(first));
    grid.add(floor(1, Hue.PLAIN).ownedBy(second));
    assertEquals(2, grid.networks().size());
  }

  private static Stream<Arguments> differentOwners() {
    return Stream.of(Arguments.of(ALICE, BOB), Arguments.of(ALICE, null), Arguments.of(null, BOB));
  }

  @Test
  void twoOwnersCanHoldTheSameFaceAndColour() {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.add(sealed(0, ALICE));
    grid.add(sealed(0, BOB));
    assertEquals(3, grid.networks().size());
    assertEquals(3, grid.cellsOn(floor(0, Hue.PLAIN).face().pos()).size());
  }

  @Test
  void aSplitSealedNetworkStaysSealedOnBothSides() {
    WireGrid grid = new WireGrid();
    IntStream.range(0, 5).forEach(x -> grid.add(sealed(x, ALICE)));
    grid.remove(sealed(2, ALICE));
    assertEquals(2, grid.networks().size());
    grid.networks().forEach(network -> assertEquals(ALICE, network.owner()));
  }

  @Test
  void reowningMovesEveryCellAndKeepsState() {
    WireGrid grid = line(3);
    WireNetwork network = grid.networkOf(floor(1, Hue.PLAIN));
    network.setOn(true);
    network.setTripped(true);
    grid.drainDirty();
    assertTrue(grid.reown(network, ALICE).isEmpty());
    assertFalse(grid.contains(floor(1, Hue.PLAIN)));
    assertSame(network, grid.networkOf(sealed(1, ALICE)));
    assertEquals(ALICE, network.owner());
    assertTrue(network.isOn());
    assertTrue(network.isTripped());
    assertTrue(network.touches().size() > 0);
    assertEquals(List.of(network), grid.drainDirty());
  }

  @Test
  void reowningChangesTheChunkVersionSoViewersAreResent() {
    WireGrid grid = line(1);
    long chunk = new ChunkPos(floor(0, Hue.PLAIN).face().pos()).toLong();
    int before = grid.chunkVersion(chunk);
    grid.reown(grid.networkOf(floor(0, Hue.PLAIN)), ALICE);
    assertTrue(grid.chunkVersion(chunk) > before);
  }

  @Test
  void reowningJoinsTheNewOwnersWireAndLeavesOthersAlone() {
    WireGrid grid = new WireGrid();
    grid.add(sealed(0, ALICE));
    grid.add(floor(1, Hue.PLAIN));
    grid.add(sealed(2, BOB));
    assertEquals(2, grid.sizeAfterReowning(grid.networkOf(floor(1, Hue.PLAIN)), ALICE));
    grid.reown(grid.networkOf(floor(1, Hue.PLAIN)), ALICE);
    assertEquals(2, grid.networkOf(sealed(0, ALICE)).size());
    assertEquals(1, grid.networkOf(sealed(2, BOB)).size());
  }

  @Test
  void reowningOntoAnOccupiedFaceReportsTheSpareCell() {
    WireGrid grid = new WireGrid();
    grid.add(sealed(0, ALICE));
    grid.add(sealed(1, ALICE));
    grid.add(floor(1, Hue.PLAIN));
    grid.add(floor(2, Hue.PLAIN));
    assertEquals(List.of(floor(1, Hue.PLAIN)), grid.reown(grid.networkOf(sealed(0, ALICE)), null));
    assertEquals(3, grid.networkOf(floor(0, Hue.PLAIN)).size());
    assertEquals(1, grid.networks().size());
  }

  @Test
  void sizeAfterAddingSealedWireCountsOpenWireAndWhatThatReaches() {
    WireGrid grid = new WireGrid();
    grid.add(floor(1, Hue.PLAIN));
    grid.add(floor(2, Hue.PLAIN));
    grid.add(sealed(3, ALICE));
    grid.add(sealed(4, ALICE));
    grid.add(sealed(3, BOB));
    assertEquals(5, grid.sizeAfterAdding(sealed(0, ALICE)));
    assertEquals(3, grid.sizeAfterAdding(floor(0, Hue.PLAIN)));
  }

  @ParameterizedTest(name = "wire at x={0}")
  @ValueSource(ints = {-1, 0, 1})
  void sealedOwnersAroundFindsTheFaceAndItsNeighbours(int x) {
    WireGrid grid = new WireGrid();
    grid.add(sealed(x, ALICE));
    grid.add(floor(x, Hue.RED).ownedBy(BOB));
    assertEquals(Set.of(ALICE), grid.sealedOwnersAround(floor(0, Hue.PLAIN).face(), Hue.PLAIN));
  }

  @Test
  void sealedOwnersAroundIgnoresOpenAndDistantWire() {
    WireGrid grid = new WireGrid();
    grid.add(floor(1, Hue.PLAIN));
    grid.add(sealed(2, ALICE));
    assertEquals(Set.of(), grid.sealedOwnersAround(floor(0, Hue.PLAIN).face(), Hue.PLAIN));
  }
}
