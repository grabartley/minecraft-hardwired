package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanMovesTest {

  private static final BlockPos LOW = new BlockPos(0, 64, 0);
  private static final BlockPos MID = new BlockPos(0, 66, 0);
  private static final BlockPos TOP = new BlockPos(0, 68, 0);

  private static BlockPos far(int x) {
    return new BlockPos(x, 66, 0);
  }

  private static WireSpan span(BlockPos from, BlockPos to, Hue hue, int wire) {
    return new WireSpan(from, to, hue, null, wire);
  }

  private static int wire(List<WireSpan> spans) {
    return spans.stream().mapToInt(WireSpan::wire).sum();
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "a span rises to the new top,PLAIN,0,1,0",
    "a colour already strung to that pole is dropped,PLAIN,1,0,1",
    "another colour to that pole rises,RED,1,1,0"
  })
  void spansOnTheLowerBlocksRiseUnlessTheyWouldClash(
      String name, Hue lowerHue, int alreadyAtTop, int moved, int dropped) {
    List<WireSpan> atTop =
        alreadyAtTop == 0 ? List.of() : List.of(span(TOP, far(20), Hue.PLAIN, 3));
    List<WireSpan> lower = List.of(span(MID, far(20), lowerHue, 5));
    SpanMoves.Plan plan = SpanMoves.plan(atTop, lower, Set.of(LOW, MID), TOP);
    assertEquals(moved, plan.moved().size());
    assertEquals(dropped, plan.dropped().size());
    assertEquals(wire(lower), wire(plan.moved()) + wire(plan.dropped()));
    plan.moved().forEach(span -> assertEquals(true, span.endsAt(TOP)));
  }

  @ParameterizedTest(name = "between {0} and {1}")
  @CsvSource({"LOW,MID", "MID,TOP"})
  void aSpanBetweenTwoBlocksOfOneColumnIsDropped(String from, String to) {
    BlockPos a = from.equals("LOW") ? LOW : MID;
    BlockPos b = to.equals("MID") ? MID : TOP;
    SpanMoves.Plan plan =
        SpanMoves.plan(List.of(), List.of(span(a, b, Hue.PLAIN, 4)), Set.of(LOW, MID), TOP);
    assertEquals(List.of(), plan.moved());
    assertEquals(4, wire(plan.dropped()));
  }

  @ParameterizedTest(name = "{0} at the top, {1} rising")
  @CsvSource({"6,2,2,0", "7,2,1,1", "8,3,0,3"})
  void aPoleNeverHoldsMoreThanItsSpanLimit(int atTop, int rising, int moved, int dropped) {
    List<WireSpan> top =
        IntStream.range(0, atTop).mapToObj(i -> span(TOP, far(10 + i), Hue.byIndex(i), 2)).toList();
    List<WireSpan> lower =
        IntStream.range(0, rising)
            .mapToObj(i -> span(MID, far(30 + i), Hue.byIndex(i), 2))
            .toList();
    SpanMoves.Plan plan = SpanMoves.plan(top, lower, Set.of(LOW, MID), TOP);
    assertEquals(moved, plan.moved().size());
    assertEquals(dropped, plan.dropped().size());
    List<WireSpan> all = new ArrayList<>(plan.moved());
    all.addAll(plan.dropped());
    assertEquals(wire(lower), wire(all));
    assertEquals(atTop + moved, (int) Stream.concat(top.stream(), plan.moved().stream()).count());
  }
}
