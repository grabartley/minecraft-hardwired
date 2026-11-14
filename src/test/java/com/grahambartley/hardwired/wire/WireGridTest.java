package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.power.GridState;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
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
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest(name = "chunk {0} radius {1}: {2} networks")
  @CsvSource({
    "0, 0, 1",
    "1, 1, 1",
    "2, 1, 0",
    "3, 3, 1",
    "4, 3, 0",
    "100, 3, 1",
    "103, 3, 1",
    "104, 3, 0"
  })
  void onlyNetworksWithWireInTheSquareAreNear(int chunkX, int radius, int expected) {
    WireGrid grid = new WireGrid();
    grid.add(floor(0, Hue.PLAIN));
    grid.add(floor(100 * 16, Hue.RED));
    assertEquals(expected, grid.networksNear(new ChunkPos(chunkX, 0), radius).size());
  }

  @ParameterizedTest(name = "owner {0}: size {1}")
  @CsvSource({"true, 1", "false, 3"})
  void joiningCountsOnlyNetworksTheCellWouldReallyMerge(boolean sealed, int expected) {
    WireGrid grid = line(2);
    UUID owner = sealed ? new UUID(1, 1) : null;
    WireCell next = new WireCell(floor(2, Hue.PLAIN).face(), Hue.PLAIN, owner);
    assertEquals(expected, grid.sizeAfterJoining(next));
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

  private static final int GROUND = 63;
  private static final UUID OWNER = new UUID(7, 7);

  private static BlockPos top(int x, int height) {
    return new BlockPos(x, GROUND + height, 0);
  }

  private static LongList column(int x, int height) {
    LongList blocks = new LongArrayList();
    for (int y = GROUND + 1; y <= GROUND + height; y++) {
      blocks.add(new BlockPos(x, y, 0).asLong());
    }
    return blocks;
  }

  private static WireCell ground(int x, Hue hue, UUID owner) {
    return new WireCell(new WireFace(new BlockPos(x, GROUND, 0), Direction.UP), hue, owner);
  }

  private static WireGrid twoPoles(int height) {
    WireGrid grid = new WireGrid();
    grid.setColumn(top(0, height), column(0, height));
    grid.setColumn(top(20, height), column(20, height));
    grid.add(ground(0, Hue.PLAIN, null));
    grid.add(ground(20, Hue.PLAIN, null));
    return grid;
  }

  private static WireSpan span(int height, Hue hue, UUID owner) {
    return new WireSpan(top(0, height), top(20, height), hue, owner, 5);
  }

  @ParameterizedTest(name = "pole height {0}")
  @ValueSource(ints = {1, 3, 6})
  void aSpanJoinsTheWireAtEachPoleIntoOneNetwork(int height) {
    WireGrid grid = twoPoles(height);
    assertEquals(2, grid.networks().size());
    grid.addSpan(span(height, Hue.PLAIN, null));
    assertEquals(1, grid.networks().size());
    assertEquals(
        grid.networkOf(ground(0, Hue.PLAIN, null)), grid.networkOf(ground(20, Hue.PLAIN, null)));
  }

  @ParameterizedTest(name = "pole height {0}")
  @ValueSource(ints = {1, 4})
  void removingTheSpanSplitsTheLineAndDropsItsAnchors(int height) {
    WireGrid grid = twoPoles(height);
    WireSpan span = span(height, Hue.PLAIN, null);
    grid.addSpan(span);
    assertTrue(grid.removeSpan(span));
    assertEquals(2, grid.networks().size());
    assertFalse(grid.contains(span.fromAnchor()));
    assertFalse(grid.contains(span.toAnchor()));
  }

  @ParameterizedTest(name = "span {0}")
  @CsvSource({"RED", "BLUE"})
  void aSpanOfAnotherColourLeavesTheWireApart(Hue hue) {
    WireGrid grid = twoPoles(3);
    grid.addSpan(span(3, hue, null));
    assertTrue(
        grid.networkOf(ground(0, Hue.PLAIN, null)) != grid.networkOf(ground(20, Hue.PLAIN, null)));
  }

  @ParameterizedTest(name = "sealed span {0}")
  @ValueSource(booleans = {true, false})
  void onlyWireInTheSpansLayerJoinsIt(boolean sealed) {
    WireGrid grid = twoPoles(2);
    grid.addSpan(span(2, Hue.PLAIN, sealed ? OWNER : null));
    boolean joined =
        grid.networkOf(ground(0, Hue.PLAIN, null)) == grid.networkOf(ground(20, Hue.PLAIN, null));
    assertEquals(!sealed, joined);
  }

  @ParameterizedTest(name = "pole cut to {0} blocks")
  @CsvSource({"3,true", "1,true"})
  void wireTouchingAnyBlockOfThePoleStaysJoinedWhileThatBlockStands(int kept, boolean joined) {
    WireGrid grid = twoPoles(3);
    grid.addSpan(span(3, Hue.PLAIN, null));
    LongList shorter = new LongArrayList(column(0, 3).subList(3 - kept, 3));
    grid.setColumn(top(0, 3), shorter);
    boolean bottomKept = shorter.contains(new BlockPos(0, GROUND + 1, 0).asLong());
    assertEquals(
        bottomKept && joined,
        grid.networkOf(ground(0, Hue.PLAIN, null)) == grid.networkOf(ground(20, Hue.PLAIN, null)));
  }

  @ParameterizedTest(name = "{0} spans")
  @ValueSource(ints = {1, 2})
  void aPoleKeepsItsAnchorWhileAnySpanOfThatColourRemains(int spans) {
    WireGrid grid = twoPoles(2);
    grid.setColumn(top(40, 2), column(40, 2));
    WireSpan first = span(2, Hue.PLAIN, null);
    WireSpan second = new WireSpan(top(20, 2), top(40, 2), Hue.PLAIN, null, 5);
    grid.addSpan(first);
    if (spans == 2) {
      grid.addSpan(second);
    }
    grid.removeSpan(first);
    assertEquals(spans == 2, grid.contains(WireSpan.anchor(top(20, 2), Hue.PLAIN, null)));
  }

  @ParameterizedTest(name = "twice {0}")
  @ValueSource(booleans = {true, false})
  void aSecondSpanOfOneColourBetweenTheSamePolesIsRefused(boolean reversed) {
    WireGrid grid = twoPoles(2);
    assertTrue(grid.addSpan(span(2, Hue.PLAIN, null)));
    WireSpan again =
        reversed
            ? new WireSpan(top(20, 2), top(0, 2), Hue.PLAIN, null, 5)
            : span(2, Hue.PLAIN, null);
    assertFalse(grid.addSpan(again));
  }

  @ParameterizedTest(name = "pole height {0}")
  @ValueSource(ints = {1, 3})
  void sealingTheLineSealsItsSpanAndKeepsItJoined(int height) {
    WireGrid grid = twoPoles(height);
    grid.addSpan(span(height, Hue.PLAIN, null));
    UUID owner = new UUID(4, 2);
    grid.reown(grid.networkOf(ground(0, Hue.PLAIN, null)), owner);
    assertEquals(Set.of(span(height, Hue.PLAIN, owner)), grid.spans().all());
    assertEquals(1, grid.networks().size());
    assertEquals(
        grid.networkOf(ground(0, Hue.PLAIN, owner)), grid.networkOf(ground(20, Hue.PLAIN, owner)));
    assertTrue(grid.contains(span(height, Hue.PLAIN, owner).toAnchor()));
  }

  @ParameterizedTest(name = "age {0} waxed {1}")
  @CsvSource({"1,false", "3,true"})
  void restylingASpanKeepsTheLineJoined(int age, boolean waxed) {
    WireGrid grid = twoPoles(2);
    WireSpan span = span(2, Hue.PLAIN, null);
    grid.addSpan(span);
    WireSpan next = new WireSpan(span.from(), span.to(), Hue.PLAIN, null, 5, age, waxed);
    assertTrue(grid.spans().restyle(span, next));
    assertEquals(Set.of(next), grid.spans().all());
    assertEquals(1, grid.networks().size());
  }
}
