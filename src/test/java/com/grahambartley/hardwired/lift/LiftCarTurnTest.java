package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftCarTurnTest {

  @ParameterizedTest(name = "gate facing {0} turns the car {1}")
  @CsvSource({"SOUTH,0", "EAST,1", "NORTH,2", "WEST,3"})
  void theCarsBackFacesAwayFromTheGate(Direction facing, int turns) {
    assertEquals(turns, LiftCarTurn.quarterTurns(facing));
  }
}
