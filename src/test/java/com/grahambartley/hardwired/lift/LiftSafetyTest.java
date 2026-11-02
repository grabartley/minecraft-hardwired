package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LiftSafetyTest {

  private static final double SLOW_FALL_BLOCKS_PER_TICK = 0.49;

  @ParameterizedTest(name = "{0},{1},{2} span {3}")
  @CsvSource({
    "0.4,10,0.4,1,true",
    "0.6,10,0,1,false",
    "0.6,10,0,2,true",
    "0,10.9,0,1,true",
    "0,12,0,1,false",
    "0,9,0,1,false"
  })
  void aPlayerIsInTheCarOnlyInsideItsSpaceAndUnderTheRoof(
      double x, double y, double z, int span, boolean inside) {
    assertEquals(inside, LiftSafety.inCar(x, y, z, 0, 10, 0, span));
  }

  @ParameterizedTest(name = "drop {0}")
  @ValueSource(doubles = {0, 1, 5.5, 60, 380})
  void slowFallingLastsPastTheLanding(double drop) {
    assertTrue(LiftSafety.slowFallTicks(drop) > drop / SLOW_FALL_BLOCKS_PER_TICK);
  }

  @ParameterizedTest(name = "{0},{1}")
  @CsvSource({
    "3.5,7.5,true",
    "3.01,7.99,true",
    "2.99,7.5,false",
    "3.5,8.0,false",
    "-0.5,7.5,false"
  })
  void theColumnIsOneBlock(double x, double z, boolean inside) {
    assertEquals(inside, LiftSafety.inColumn(x, z, 3, 7, 1));
  }

  @ParameterizedTest(name = "{0},{1}")
  @CsvSource({"3.5,7.5,true", "4.9,8.9,true", "5.0,7.5,false", "3.5,9.0,false", "2.9,8.0,false"})
  void aLargeColumnIsTwoBlocksEachWay(double x, double z, boolean inside) {
    assertEquals(inside, LiftSafety.inColumn(x, z, 3, 7, 2));
  }
}
