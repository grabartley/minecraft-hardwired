package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LampRateLimiterTest {

  @ParameterizedTest(name = "changed at {0}, now {1}: wait {2}")
  @CsvSource({"100,100,7", "100,103,4", "100,107,0", "100,500,0"})
  void lampsChangeAtMostThreeTimesASecond(long last, long now, long wait) {
    assertEquals(wait, LampRateLimiter.ticksUntilAllowed(last, now));
  }
}
