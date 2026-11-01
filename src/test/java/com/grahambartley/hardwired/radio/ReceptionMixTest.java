package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReceptionMixTest {

  @ParameterizedTest(name = "volume {0} strength {1}")
  @CsvSource({"100,1,1", "50,1,0.5", "0,1,0", "100,0,0.35", "100,0.5,0.675", "200,1,1", "-5,1,0"})
  void aStationGetsQuieterWithTheKnobAndWithWeakSignal(
      int volume, double strength, double expected) {
    assertEquals(expected, ReceptionMix.stationLoudness(volume, strength), 1e-9);
  }
}
