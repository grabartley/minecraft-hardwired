package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanRulesTest {

  @ParameterizedTest(name = "{0} blocks at {1} a wire")
  @CsvSource({"1,4,1", "4,4,1", "4.1,4,2", "16,4,4", "31.9,8,4", "32,1,32"})
  void aSpanCostsOneWirePerStartedStretch(double length, int perWire, int cost) {
    assertEquals(cost, SpanRules.cost(length, perWire));
  }

  @ParameterizedTest(name = "{0} blocks within {1}")
  @CsvSource({"0,32,false", "1,32,true", "32,32,true", "32.5,32,false"})
  void aSpanReachesOnlyAsFarAsTheSetting(double length, int max, boolean reaches) {
    assertEquals(reaches, SpanRules.reaches(length, max));
  }

  @ParameterizedTest(name = "{0} spans")
  @CsvSource({"0,true", "7,true", "8,false", "9,false"})
  void aPoleHoldsUpToEightSpans(int spans, boolean room) {
    assertEquals(room, SpanRules.hasRoom(spans));
  }

  @ParameterizedTest(name = "{0} wire in stacks of {1}")
  @CsvSource({"0,64,''", "5,64,5", "64,64,64", "65,64,'64,1'", "130,64,'64,64,2'", "9,4,'4,4,1'"})
  void returnedWireComesBackAsFullStacksAndTheRest(int count, int max, String stacks) {
    List<Integer> expected =
        stacks.isEmpty()
            ? List.of()
            : Arrays.stream(stacks.split(",")).map(Integer::valueOf).toList();
    List<Integer> sizes = SpanRules.stackSizes(count, max);
    assertEquals(expected, sizes);
    assertEquals(count, sizes.stream().mapToInt(Integer::intValue).sum());
  }

  @ParameterizedTest(name = "{11}")
  @CsvSource({
    "true,0,32,0,0,false,1,10,0,1,false,CANCEL",
    "false,40,32,0,0,false,1,10,64,10,false,TOO_FAR",
    "false,20,32,8,0,false,1,10,64,5,false,POLE_FULL",
    "false,20,32,0,8,false,1,10,64,5,false,POLE_FULL",
    "false,20,32,1,1,true,1,10,64,5,false,ALREADY_STRUNG",
    "false,20,32,1,1,false,11,10,64,5,true,NETWORK_FULL",
    "false,20,32,1,1,false,10,10,4,5,false,NEEDS_WIRE",
    "false,20,32,1,1,false,10,10,0,5,true,STRUNG",
    "false,20,32,7,7,false,1,10,5,5,false,STRUNG"
  })
  void finishingASpanChecksEachRuleInOrder(
      boolean samePole,
      double length,
      int max,
      int atStart,
      int atEnd,
      boolean strung,
      int sizeAfter,
      int maxSize,
      int held,
      int cost,
      boolean creative,
      SpanRules.Finish expected) {
    assertEquals(
        expected,
        SpanRules.finish(
            new SpanRules.Attempt(
                samePole, length, max, atStart, atEnd, strung, sizeAfter, maxSize, held, cost,
                creative)));
  }
}
