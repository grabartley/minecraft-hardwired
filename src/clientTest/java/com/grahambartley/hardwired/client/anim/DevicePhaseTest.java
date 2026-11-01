package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DevicePhaseTest {

  @ParameterizedTest(name = "speed {0}")
  @CsvSource({"0", "1", "2"})
  void theFirstFrameDoesNotJump(float speed) {
    assertEquals(0F, new DevicePhase.Phase().advance(speed, 500D));
  }

  @ParameterizedTest(name = "speed {0} for {1} ticks")
  @CsvSource({"1,100", "2,400"})
  void speedEasesInSoThePhaseNeverJumps(float speed, int ticks) {
    DevicePhase.Phase phase = new DevicePhase.Phase();
    phase.advance(speed, 0D);
    float seconds = phase.advance(speed, ticks);
    assertTrue(seconds > 0F && seconds <= speed * ticks / 20F);
  }

  @ParameterizedTest(name = "time steps back by {0}")
  @CsvSource({"0", "5"})
  void timeThatStandsStillHoldsThePhase(int back) {
    DevicePhase.Phase phase = new DevicePhase.Phase();
    phase.advance(1F, 100D);
    float seconds = phase.advance(1F, 200D);
    assertEquals(seconds, phase.advance(1F, 200D - back));
  }
}
