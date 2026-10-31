package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LiftSafetyTest {

  private static final double SLOW_FALL_BLOCKS_PER_TICK = 0.49;

  @ParameterizedTest(name = "rider {0}")
  @ValueSource(ints = {0, 1, 2, 3, 4, 7})
  void seatsStayInsideTheCar(int rider) {
    assertTrue(Math.abs(LiftSafety.seatX(rider)) < 0.5);
    assertTrue(Math.abs(LiftSafety.seatZ(rider)) < 0.5);
  }

  @ParameterizedTest(name = "riders {0} and {1}")
  @CsvSource({"0,1", "1,2", "2,3", "0,3"})
  void nextRidersStandApart(int first, int second) {
    assertNotEquals(
        LiftSafety.seatX(first) * 2 + LiftSafety.seatZ(first),
        LiftSafety.seatX(second) * 2 + LiftSafety.seatZ(second));
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
    assertEquals(inside, LiftSafety.inColumn(x, z, 3, 7));
  }
}
