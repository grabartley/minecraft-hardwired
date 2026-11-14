package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireSpan;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanReachTest {

  private static WireSpan span(int fromX, int toX, int z) {
    return new WireSpan(new BlockPos(fromX, 70, z), new BlockPos(toX, 70, z), Hue.PLAIN, null, 4);
  }

  @ParameterizedTest(name = "player at {0},{1}")
  @CsvSource({"15,0,0", "15,5,25", "-3,0,9", "40,4,116", "0,0,0"})
  void distanceIsToTheNearestPointOfTheRunAcrossTheGround(int x, int z, double squared) {
    assertEquals(squared, SpanReach.distanceSquared(span(0, 30, 0), new BlockPos(x, 200, z)), 1e-9);
  }

  @ParameterizedTest(name = "range {0} most {1}")
  @CsvSource({"200,9,'0,1,2'", "20,9,'0,1'", "5,9,'0'", "200,2,'0,1'", "1,9,''"})
  void onlyTheNearestSpansInRangeAreSent(int range, int most, String expected) {
    List<WireSpan> spans = List.of(span(0, 30, 100), span(0, 30, 3), span(0, 30, 15));
    List<WireSpan> sorted = List.of(spans.get(1), spans.get(2), spans.get(0));
    List<WireSpan> want =
        expected.isEmpty()
            ? List.of()
            : Arrays.stream(expected.split(",")).map(i -> sorted.get(Integer.parseInt(i))).toList();
    assertEquals(want, SpanReach.near(spans, new BlockPos(10, 64, 0), range, most));
  }
}
