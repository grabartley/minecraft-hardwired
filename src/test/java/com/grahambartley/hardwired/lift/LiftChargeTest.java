package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.device.DeviceStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftChargeTest {

  @ParameterizedTest(name = "started {0} paid {1}")
  @CsvSource({"0,0,false", "1,0,true", "1,1,false", "4,3,true"})
  void aBlockIsOwedOnceWhenTheCarEntersIt(int started, int paid, boolean owes) {
    assertEquals(owes, LiftCharge.owes(started, paid));
  }

  @ParameterizedTest(name = "cell {0}")
  @CsvSource({"true,NO_CHARGE", "false,NO_CELL"})
  void aCarThatCannotPaySaysWhy(boolean hasCell, DeviceStatus reason) {
    assertEquals(reason, LiftCharge.stopReason(hasCell));
  }

  @ParameterizedTest(name = "{0} cost {2} cell {3} stored {4}")
  @CsvSource({
    "MOVING,READY,2,true,0,RUNNING",
    "STOPPED,SHAFT_BLOCKED,2,true,50,SHAFT_BLOCKED",
    "STOPPED,NO_CHARGE,2,true,0,NO_CHARGE",
    "RESTING,READY,2,true,2,READY",
    "RESTING,READY,2,true,1,NO_CHARGE",
    "RESTING,READY,2,false,0,NO_CELL",
    "RESTING,READY,0,false,0,READY",
    "LOWERING,NO_CHARGE,2,true,0,NO_CHARGE"
  })
  void aStationaryLiftShowsWhetherItCouldRun(
      LiftPhase phase,
      DeviceStatus reason,
      int cost,
      boolean hasCell,
      int stored,
      DeviceStatus shown) {
    assertEquals(shown, LiftCharge.demand(phase, reason, cost, hasCell, stored));
  }
}
