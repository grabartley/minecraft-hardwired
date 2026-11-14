package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireGridSpanTest {

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
