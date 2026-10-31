package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.grahambartley.hardwired.circuit.Side;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class CircuitSidesTest {

  @ParameterizedTest(name = "{0} round trips")
  @EnumSource(Side.class)
  void sidesRoundTrip(Side side) {
    assertEquals(side, CircuitSides.side(CircuitSides.direction(side)));
  }

  @ParameterizedTest(name = "{0} is no side of a Circuit Block")
  @EnumSource(
      value = Direction.class,
      names = {"UP", "DOWN"})
  void topAndBottomAreNoSide(Direction direction) {
    assertNull(CircuitSides.side(direction));
  }

  @ParameterizedTest(name = "facing {0} turns {1}")
  @CsvSource({"NORTH, 0", "EAST, 1", "SOUTH, 2", "WEST, 3", "UP, 0"})
  void facingGivesTheTurns(Direction facing, int turns) {
    assertEquals(turns, CircuitSides.turnsFor(facing));
  }
}
