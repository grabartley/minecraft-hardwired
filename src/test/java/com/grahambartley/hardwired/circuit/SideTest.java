package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SideTest {

  @ParameterizedTest(name = "{0} turned {1} is {2}")
  @CsvSource({
    "NORTH, 0, NORTH",
    "NORTH, 1, EAST",
    "NORTH, 2, SOUTH",
    "NORTH, 3, WEST",
    "WEST, 1, NORTH",
    "EAST, -1, NORTH",
    "SOUTH, -3, WEST",
    "SOUTH, 6, NORTH"
  })
  void sidesTurnClockwise(Side side, int turns, Side expected) {
    assertEquals(expected, side.turned(turns));
  }

  @ParameterizedTest(name = "{0} has bit {1}")
  @CsvSource({"NORTH, 1", "EAST, 2", "SOUTH, 4", "WEST, 8"})
  void eachSideHasItsOwnBit(Side side, int bit) {
    assertEquals(bit, side.bit());
  }
}
