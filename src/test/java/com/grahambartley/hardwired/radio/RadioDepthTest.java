package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioDepthTest {

  private static final Map<Integer, Double> ROOF = Map.of(63, 1D, 40, 8D, 20, 0.5D);

  @ParameterizedTest(name = "top {0}, radio {1}")
  @CsvSource({"64,10,9.5", "64,30,9", "64,50,1", "64,63,0", "64,70,0"})
  void onlyLayersAboveTheRadioCountAndAirCountsNothing(int top, int radio, double depth) {
    assertEquals(depth, RadioDepth.above(top, radio, y -> ROOF.getOrDefault(y, 0D)), 1E-9);
  }
}
