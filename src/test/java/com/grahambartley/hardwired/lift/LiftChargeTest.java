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

  @ParameterizedTest(name = "{0} stop {1} power {2}")
  @CsvSource({
    "MOVING,READY,OVERLOADED,RUNNING",
    "STOPPED,SHAFT_BLOCKED,READY,SHAFT_BLOCKED",
    "STOPPED,OUT_OF_FUEL,READY,OUT_OF_FUEL",
    "RESTING,READY,READY,READY",
    "RESTING,READY,NO_SOURCE,NO_SOURCE",
    "LOWERING,OUT_OF_FUEL,OVERLOADED,OVERLOADED"
  })
  void aStationaryLiftShowsWhetherItCouldRun(
      LiftPhase phase, DeviceStatus reason, DeviceStatus power, DeviceStatus shown) {
    assertEquals(shown, LiftCharge.demand(phase, reason, power));
  }
}
