package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GridRuleTest {

  @ParameterizedTest(name = "load {0} supply {1} sources {2} off {3} starting {4}")
  @CsvSource({
    "1,16,1,0,false,100,0,LIVE",
    "16,16,1,0,false,100,0,LIVE",
    "17,16,1,0,false,100,0,OVERLOADED",
    "1,0,0,0,false,100,0,NO_SOURCE",
    "0,0,0,0,false,100,0,NO_SOURCE",
    "1,0,2,2,false,100,0,SOURCES_OFF",
    "1,0,2,1,false,100,0,OUT_OF_FUEL",
    "1,0,1,0,false,100,0,OUT_OF_FUEL",
    "1,16,1,0,false,100,140,RETURNING",
    "17,16,1,0,false,100,140,OVERLOADED",
    "1,16,1,0,false,140,140,LIVE",
    "50,16,2,0,true,100,0,RETURNING"
  })
  void theGridFollowsItsCount(
      int load,
      int supply,
      int sources,
      int off,
      boolean starting,
      long now,
      long darkUntil,
      GridState state) {
    GridRule.Count count = new GridRule.Count(load, supply, sources, off, starting);
    assertEquals(state, GridRule.next(count, now, darkUntil));
  }

  @ParameterizedTest(name = "{0} to {1}")
  @CsvSource({
    "LIVE,OVERLOADED,100,0,140",
    "LIVE,OUT_OF_FUEL,100,0,140",
    "LIVE,LIVE,100,0,0",
    "OVERLOADED,RETURNING,120,140,140",
    "NO_SOURCE,LIVE,100,0,0"
  })
  void goingDarkHoldsForTwoSeconds(
      GridState before, GridState after, long now, long darkUntil, long held) {
    assertEquals(held, GridRule.darkUntil(before, after, now, darkUntil));
  }

  @ParameterizedTest(name = "{0} to {1}")
  @CsvSource({
    "LIVE,OVERLOADED,true",
    "NO_SOURCE,OVERLOADED,true",
    "OVERLOADED,OVERLOADED,false",
    "LIVE,OUT_OF_FUEL,false",
    "LIVE,SOURCES_OFF,false",
    "RETURNING,LIVE,false"
  })
  void onlyAnOverloadBlowsTheFuseAndOnlyOnce(GridState before, GridState after, boolean blows) {
    assertEquals(blows, GridRule.blowsFuse(before, after));
  }
}
