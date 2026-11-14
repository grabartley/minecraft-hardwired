package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WireSpanTest {

  @ParameterizedTest(name = "{0} to {1}")
  @CsvSource({"0,30", "30,0"})
  void aSpanIsTheSameWhicheverEndItStartsFrom(int a, int b) {
    WireSpan span = new WireSpan(new BlockPos(a, 70, 0), new BlockPos(b, 70, 0), Hue.RED, null, 5);
    assertEquals(
        new WireSpan(new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), Hue.RED, null, 5), span);
    assertEquals(30, span.length(), 1e-9);
  }

  @ParameterizedTest(name = "end {0}")
  @CsvSource({"0,30", "30,0"})
  void theOtherEndIsTheFarPole(int end, int other) {
    WireSpan span = new WireSpan(new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), Hue.RED, null, 5);
    assertEquals(new BlockPos(other, 70, 0), span.otherEnd(new BlockPos(end, 70, 0)));
  }

  @ParameterizedTest(name = "end {0} rises to {1}")
  @CsvSource({"0,72", "30,75"})
  void movingAnEndKeepsTheOther(int end, int newY) {
    WireSpan span = new WireSpan(new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), Hue.RED, null, 5);
    WireSpan moved = span.movedEnd(new BlockPos(end, 70, 0), new BlockPos(end, newY, 0));
    assertEquals(
        new BlockPos(end, newY, 0),
        moved.endsAt(new BlockPos(end, newY, 0)) ? new BlockPos(end, newY, 0) : null);
    assertEquals(new BlockPos(30 - end, 70, 0), moved.otherEnd(new BlockPos(end, newY, 0)));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({"PLAIN", "BLUE"})
  void anAnchorSitsOnTopOfThePoleInTheSpansColour(Hue hue) {
    WireCell anchor = WireSpan.anchor(new BlockPos(3, 70, 4), hue, null);
    assertEquals(new WireFace(new BlockPos(3, 70, 4), Direction.UP), anchor.face());
    assertEquals(hue, anchor.color());
  }

  @ParameterizedTest(name = "{0} to {1} {2} paid {3}")
  @CsvSource({"30,0,RED,9,true", "0,30,RED,1,true", "0,30,BLUE,5,false", "0,31,RED,5,false"})
  void aRunIsTheSamePolesAndColourWhateverItCost(int a, int b, Hue color, int wire, boolean same) {
    WireSpan span = new WireSpan(new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), Hue.RED, null, 5);
    WireSpan other =
        new WireSpan(new BlockPos(a, 70, 0), new BlockPos(b, 70, 0), color, new UUID(1, 1), wire);
    assertEquals(same, span.sameRun(other));
  }

  @ParameterizedTest(name = "{0} age {1} waxed {2}")
  @CsvSource({
    "PLAIN,0,false,true",
    "PLAIN,2,false,true",
    "PLAIN,3,false,false",
    "PLAIN,0,true,false",
    "RED,0,false,false"
  })
  void onlyPlainUnwaxedCopperAges(Hue color, int age, boolean waxed, boolean ages) {
    WireSpan span =
        new WireSpan(new BlockPos(0, 70, 0), new BlockPos(30, 70, 0), color, null, 5, age, waxed);
    assertEquals(ages, span.canAge());
    assertEquals(Math.min(age + 1, WireSpan.MAX_AGE), span.older().age());
    assertEquals(true, span.withWax().waxed());
    assertEquals(age, span.withWax().age());
    assertEquals(span.movedEnd(span.from(), span.from().up()).age(), age);
  }
}
