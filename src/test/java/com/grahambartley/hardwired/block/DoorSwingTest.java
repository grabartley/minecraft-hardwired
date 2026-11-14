package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DoorSwingTest {

  @ParameterizedTest(name = "open {0} after {1} ticks is {2}")
  @CsvSource({
    "true,0,0",
    "true,5,0.5",
    "true,10,1",
    "true,40,1",
    "false,0,1",
    "false,5,0.5",
    "false,10,0",
    "true,-3,0"
  })
  void theDoorEasesAcrossHalfASecond(boolean open, float ticks, float openness) {
    assertEquals(openness, DoorSwing.openness(open, ticks), 1e-6);
  }
}
