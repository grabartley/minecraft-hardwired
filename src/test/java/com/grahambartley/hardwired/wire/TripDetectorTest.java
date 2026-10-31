package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TripDetectorTest {

  @ParameterizedTest(name = "{0} changes every {1} ticks trips: {2}")
  @CsvSource({"10,1,false", "11,1,true", "11,2,false", "20,2,false", "30,1,true"})
  void tripsOnMoreThanTenChangesInTwentyTicks(int changes, int spacing, boolean expected) {
    TripDetector detector = new TripDetector();
    boolean tripped = false;
    for (int i = 0; i < changes; i++) {
      tripped |= detector.recordChange(100L + i * spacing);
    }
    assertEquals(expected, tripped);
  }
}
