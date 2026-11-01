package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioDepthTest {

  @ParameterizedTest(name = "top {0}, radio {1}, top solid {2}")
  @CsvSource({"64,10,63,54", "64,10,40,31", "64,63,-1,0", "64,10,-1,0", "64,70,-1,0"})
  void depthCountsFromTheTopSolidBlock(int top, int radio, int solid, int depth) {
    assertEquals(depth, RadioDepth.below(top, radio, Set.of(solid)::contains));
  }
}
