package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerSoundLevelsTest {

  @ParameterizedTest(name = "load {0} setting {1}")
  @CsvSource({
    "0,100,0",
    "24,100,0",
    "76,100,0.5",
    "128,100,1",
    "500,100,1",
    "76,50,0.25",
    "128,0,0"
  })
  void theHumRisesFromStartToFullThenFollowsTheSetting(int load, int setting, float volume) {
    assertEquals(volume, PowerSoundLevels.hum(load, 24, 128, setting), 1E-6F);
  }

  @ParameterizedTest(name = "workload {0} to {1}")
  @CsvSource({"0,5", "5,10", "10,15"})
  void harderWorkSoundsHigherAndLouder(int lower, int higher) {
    assertTrue(PowerSoundLevels.loopPitch(lower) < PowerSoundLevels.loopPitch(higher));
    assertTrue(PowerSoundLevels.loopVolume(lower) < PowerSoundLevels.loopVolume(higher));
  }
}
