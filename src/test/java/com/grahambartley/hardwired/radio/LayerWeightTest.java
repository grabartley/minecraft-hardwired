package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LayerWeightTest {

  @ParameterizedTest(name = "hardness {0} weighs {1}")
  @CsvSource({"0,0", "0.5,0.3333", "1.5,1", "3,2", "12,8", "50,8", "-1,8"})
  void toughnessIsMeasuredAgainstStoneUpToACap(float hardness, double weight) {
    assertEquals(weight, LayerWeight.of(hardness), 1E-3);
  }
}
