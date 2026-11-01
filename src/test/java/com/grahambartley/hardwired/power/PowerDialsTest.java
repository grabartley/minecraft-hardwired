package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerDialsTest {

  private static final float CLOSE = 0.0001F;

  @ParameterizedTest(name = "workload {0}")
  @CsvSource({"0,0", "15,1", "3,0.2"})
  void theGaugeSpansTheWorkload(int workload, float value) {
    assertEquals(value, PowerDials.gauge(workload), CLOSE);
  }

  @ParameterizedTest(name = "running {0} workload {1}")
  @CsvSource({"false,15,0", "true,0,0.35", "true,15,2"})
  void clipsIdleThenSpeedUp(boolean running, int workload, float speed) {
    assertEquals(speed, PowerDials.clipSpeed(running, workload), CLOSE);
  }

  @ParameterizedTest(name = "sky angle {0}")
  @CsvSource({"0.75,1", "0.76,0.02", "0,0.5", "0.25,1", "0.5,1"})
  void theSunClimbsAcrossTheDay(float angle, float phase) {
    assertEquals(phase, PowerDials.sunPhase(angle), CLOSE);
  }

  @ParameterizedTest(name = "workload {0} at time {1}")
  @CsvSource({"0,0", "8,11", "15,27", "15,100"})
  void aLiveNeedleStaysInRange(int workload, float time) {
    float needle = PowerDials.needle(true, workload, time);
    assertTrue(needle >= 0.1F && needle <= 1F);
    assertEquals(0F, PowerDials.needle(false, workload, time));
  }

  @ParameterizedTest(name = "closed {0} after {1} ticks")
  @CsvSource({"true,0,0", "true,1000,1", "false,0,1", "false,1000,0"})
  void theLeverTravelsOneWayEachThrow(boolean closed, float ticks, float value) {
    assertEquals(value, PowerDials.thrown(closed, ticks), CLOSE);
  }

  @ParameterizedTest(name = "from {0} to {1}, running {2}, {3} ticks")
  @CsvSource({
    "0.5,0.9,true,10,0.6",
    "0.5,0.1,true,10,0.4",
    "0.5,0.51,true,10,0.51",
    "0.5,0.9,false,10,0.5",
    "0.5,0.9,true,-5,0.5"
  })
  void aRunningPanelEasesToTheSunAndAStoppedOneHolds(
      float current, float target, boolean running, float ticks, float next) {
    assertEquals(next, PowerDials.easeSun(current, target, running, ticks), 1E-5F);
  }
}
