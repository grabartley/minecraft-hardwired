package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireSpansTest {

  private static final BlockPos WEST = new BlockPos(0, 70, 0);
  private static final BlockPos EAST = new BlockPos(20, 70, 0);
  private static final BlockPos NORTH = new BlockPos(0, 70, -20);
  private static final UUID OWNER = new UUID(3, 3);

  private static WireSpan span(BlockPos from, BlockPos to, Hue hue, UUID owner) {
    return new WireSpan(from, to, hue, owner, 5);
  }

  @ParameterizedTest(name = "{0} then {1}")
  @CsvSource({"PLAIN,PLAIN,false", "PLAIN,RED,true"})
  void aPairOfPolesTakesOneSpanPerColour(Hue first, Hue second, boolean added) {
    WireSpans spans = new WireSpans();
    spans.add(span(WEST, EAST, first, null));
    assertEquals(added, spans.add(span(EAST, WEST, second, OWNER)));
  }

  @ParameterizedTest(name = "at {0}")
  @CsvSource({"WEST,2", "EAST,1", "NORTH,1"})
  void spansAreFoundByEitherEnd(String where, int count) {
    WireSpans spans = new WireSpans();
    spans.add(span(WEST, EAST, Hue.PLAIN, null));
    spans.add(span(WEST, NORTH, Hue.RED, null));
    BlockPos top =
        switch (where) {
          case "WEST" -> WEST;
          case "EAST" -> EAST;
          default -> NORTH;
        };
    assertEquals(count, spans.at(top).size());
    spans.remove(span(WEST, EAST, Hue.PLAIN, null));
    assertEquals(where.equals("EAST") ? 0 : 1, spans.at(top).size());
  }

  @ParameterizedTest(name = "owner {0}")
  @ValueSource(booleans = {true, false})
  void anAnchorMatchesItsSpansColourAndOwner(boolean sealed) {
    WireSpans spans = new WireSpans();
    UUID owner = sealed ? OWNER : null;
    spans.add(span(WEST, EAST, Hue.PLAIN, owner));
    assertEquals(true, spans.isAnchor(WireSpan.anchor(WEST, Hue.PLAIN, owner)));
    assertEquals(false, spans.isAnchor(WireSpan.anchor(WEST, Hue.RED, owner)));
    assertEquals(false, spans.isAnchor(WireSpan.anchor(WEST, Hue.PLAIN, new UUID(9, 9))));
    assertEquals(
        List.of(WireSpan.anchor(EAST, Hue.PLAIN, owner)),
        spans.linkedTo(WireSpan.anchor(WEST, Hue.PLAIN, owner)));
  }

  @ParameterizedTest(name = "touching block {0}")
  @CsvSource({"69,true", "68,true", "71,false"})
  void wireTouchingAColumnFindsItsAnchor(int y, boolean found) {
    WireSpans spans = new WireSpans();
    spans.add(span(WEST, EAST, Hue.PLAIN, null));
    spans.setColumn(
        WEST,
        new LongArrayList(new long[] {WEST.down(2).asLong(), WEST.down().asLong(), WEST.asLong()}));
    WireCell touching =
        new WireCell(new WireFace(new BlockPos(1, y, 0), Direction.WEST), Hue.PLAIN);
    WireCell anchor = spans.anchorFor(new BlockPos(0, y, 0).asLong(), touching);
    assertEquals(found ? WireSpan.anchor(WEST, Hue.PLAIN, null) : null, anchor);
    spans.clearColumn(WEST);
    assertNull(spans.anchorFor(new BlockPos(0, y, 0).asLong(), touching));
  }

  @ParameterizedTest(name = "age {0}")
  @ValueSource(ints = {1, 3})
  void restylingAndReowningKeepTheIndex(int age) {
    WireSpans spans = new WireSpans();
    WireSpan span = span(WEST, EAST, Hue.PLAIN, null);
    spans.add(span);
    WireSpan aged = new WireSpan(WEST, EAST, Hue.PLAIN, null, 5, age, false);
    assertEquals(true, spans.restyle(span, aged));
    spans.reown(List.of(WireSpan.anchor(WEST, Hue.PLAIN, null)), OWNER);
    assertEquals(Set.of(aged.ownedBy(OWNER)), spans.all());
    assertEquals(List.of(aged.ownedBy(OWNER)), spans.at(EAST));
  }
}
