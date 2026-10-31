package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireViewerTest {

  private static final UUID ALICE = new UUID(1, 1);
  private static final UUID BOB = new UUID(2, 2);
  private static final UUID CAROL = new UUID(3, 3);
  private static final WireFace FACE = new WireFace(new BlockPos(0, 64, 0), Direction.UP);
  private static final WireFace NEXT = new WireFace(new BlockPos(1, 64, 0), Direction.UP);

  private static Stream<Arguments> sight() {
    return Stream.of(
        Arguments.of(null, false, true),
        Arguments.of(ALICE, false, true),
        Arguments.of(BOB, false, false),
        Arguments.of(BOB, true, true));
  }

  @ParameterizedTest(name = "owner {0}, override {1}: visible {2}")
  @MethodSource("sight")
  void aSealedCellShowsOnlyToItsOwnerOrAnOverride(UUID owner, boolean override, boolean visible) {
    assertEquals(
        visible, new WireViewer(ALICE, override).canSee(new WireCell(FACE, Hue.RED, owner)));
  }

  @Test
  void hiddenCellsAreLeftOutAndTheRestAreRankedOwnFirst() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(FACE, Hue.RED));
    grid.add(new WireCell(FACE, Hue.RED, ALICE));
    grid.add(new WireCell(FACE, Hue.RED, BOB));
    assertEquals(
        List.of(new WireCell(FACE, Hue.RED, ALICE), new WireCell(FACE, Hue.RED)),
        new WireViewer(ALICE, false).visibleOn(grid, FACE));
    assertEquals(
        List.of(
            new WireCell(FACE, Hue.RED, ALICE),
            new WireCell(FACE, Hue.RED),
            new WireCell(FACE, Hue.RED, BOB)),
        new WireViewer(ALICE, true).visibleOn(grid, FACE));
  }

  @Test
  void aHiddenCellOfTheHeldColourNeverDecidesTheChoice() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(FACE, Hue.RED, BOB));
    grid.add(new WireCell(FACE, Hue.BLUE));
    WireViewer viewer = new WireViewer(ALICE, false);
    assertEquals(Optional.empty(), viewer.find(grid, FACE, Hue.RED));
    assertEquals(Optional.of(new WireCell(FACE, Hue.BLUE)), viewer.choose(grid, FACE, Hue.RED));
  }

  @Test
  void onlyHiddenWireGivesNothingToChoose() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(FACE, Hue.RED, BOB));
    assertEquals(Optional.empty(), new WireViewer(ALICE, false).choose(grid, FACE, Hue.RED));
  }

  @ParameterizedTest(name = "override {0}")
  @ValueSource(booleans = {true, false})
  void wireBesideOnlyAStrangersSealStaysOpenUnlessOverrideIsOn(boolean override) {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(NEXT, Hue.RED, BOB));
    UUID owner = new WireViewer(ALICE, override).ownerOfNewWire(grid, FACE, Hue.RED);
    assertEquals(override ? BOB : null, owner);
  }

  @Test
  void anOwnersWireJoinsTheirOwnSealBeforeAnyOther() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(NEXT, Hue.RED, ALICE));
    grid.add(new WireCell(NEXT, Hue.RED, BOB));
    assertEquals(BOB, new WireViewer(BOB, true).ownerOfNewWire(grid, FACE, Hue.RED));
  }

  @Test
  void anOverrideBetweenTwoSealsPicksTheSameOwnerEveryTime() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(NEXT, Hue.RED, CAROL));
    grid.add(new WireCell(NEXT, Hue.RED, BOB));
    assertEquals(BOB, new WireViewer(ALICE, true).ownerOfNewWire(grid, FACE, Hue.RED));
  }

  @Test
  void wireOnOpenGroundIsOpen() {
    assertNull(new WireViewer(ALICE, true).ownerOfNewWire(new WireGrid(), FACE, Hue.RED));
  }

  private static Stream<Arguments> waxCases() {
    WireCell open = new WireCell(FACE, Hue.BLUE);
    WireCell mine = new WireCell(FACE, Hue.RED, ALICE);
    return Stream.of(
        Arguments.of(WireWax.Action.SEAL, Hue.BLUE, Optional.of(open)),
        Arguments.of(WireWax.Action.SEAL, Hue.RED, Optional.of(mine)),
        Arguments.of(WireWax.Action.SEAL, Hue.GREEN, Optional.of(open)),
        Arguments.of(WireWax.Action.UNSEAL, Hue.BLUE, Optional.of(mine)),
        Arguments.of(WireWax.Action.UNSEAL, Hue.RED, Optional.of(mine)));
  }

  @ParameterizedTest(name = "{0} holding {1}")
  @MethodSource("waxCases")
  void waxTargetsTheHeldColourThenOpenWireAndNeverAStrangersSeal(
      WireWax.Action action, Hue held, Optional<WireCell> expected) {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(FACE, Hue.BLUE));
    grid.add(new WireCell(FACE, Hue.RED, ALICE));
    grid.add(new WireCell(FACE, Hue.GREEN, BOB));
    WireWax.Use use = new WireWax.Use(action, Hand.MAIN_HAND, held);
    assertEquals(expected, new WireViewer(ALICE, false).waxTarget(grid, FACE, use));
  }

  @Test
  void anAxeFindsNothingOnOnlyOpenWire() {
    WireGrid grid = new WireGrid();
    grid.add(new WireCell(FACE, Hue.BLUE));
    WireWax.Use use = new WireWax.Use(WireWax.Action.UNSEAL, Hand.MAIN_HAND, Hue.BLUE);
    assertEquals(Optional.empty(), new WireViewer(ALICE, false).waxTarget(grid, FACE, use));
  }
}
