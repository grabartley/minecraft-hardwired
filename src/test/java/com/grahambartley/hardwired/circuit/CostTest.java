package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CostTest {

  @ParameterizedTest(name = "{0} parts at {1} per redstone cost {2}")
  @CsvSource({
    "0, 4, 1",
    "1, 4, 1",
    "4, 4, 1",
    "5, 4, 2",
    "8, 4, 2",
    "9, 4, 3",
    "64, 4, 16",
    "256, 1, 256",
    "256, 64, 4"
  })
  void costRoundsUp(int parts, int perRedstone, int expected) {
    assertEquals(expected, Cost.redstone(parts, perRedstone));
  }

  @Test
  void costNeverFallsAsPartsAreAdded() {
    for (int parts = 1; parts < 256; parts++) {
      assertTrue(Cost.redstone(parts + 1, 4) >= Cost.redstone(parts, 4));
    }
  }
}
