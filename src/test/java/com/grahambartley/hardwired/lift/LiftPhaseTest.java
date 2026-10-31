package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class LiftPhaseTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({"RESTING,false", "MOVING,true", "STOPPED,false", "LOWERING,true"})
  void onlyMovingAndLoweringTurnTheWinch(LiftPhase phase, boolean travelling) {
    assertEquals(travelling, phase.travelling());
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(LiftPhase.class)
  void aPhaseSurvivesItsSavedName(LiftPhase phase) {
    assertEquals(phase, LiftPhase.byName(phase.name()));
  }

  @ParameterizedTest(name = "name [{0}]")
  @ValueSource(strings = {"", "nonsense"})
  void anUnknownPhaseRests(String name) {
    assertEquals(LiftPhase.RESTING, LiftPhase.byName(name));
  }
}
