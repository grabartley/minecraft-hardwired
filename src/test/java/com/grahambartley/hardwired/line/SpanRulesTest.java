package com.grahambartley.hardwired.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

  @ParameterizedTest(name = "{9}")
  @CsvSource({
    "true,0,32,0,0,false,0,1,false,CANCEL",
    "false,40,32,0,0,false,64,10,false,TOO_FAR",
    "false,20,32,8,0,false,64,5,false,POLE_FULL",
    "false,20,32,0,8,false,64,5,false,POLE_FULL",
    "false,20,32,1,1,true,64,5,false,ALREADY_STRUNG",
    "false,20,32,1,1,false,4,5,false,NEEDS_WIRE",
    "false,20,32,1,1,false,0,5,true,STRUNG",
    "false,20,32,7,7,false,5,5,false,STRUNG"
  })
  void finishingASpanChecksEachRuleInOrder(
      boolean samePole,
      double length,
      int max,
      int atStart,
      int atEnd,
      boolean strung,
      int held,
      int cost,
      boolean creative,
      SpanRules.Finish expected) {
    assertEquals(
        expected,
        SpanRules.finish(
            new SpanRules.Attempt(
                samePole, length, max, atStart, atEnd, strung, held, cost, creative)));
  }
}
