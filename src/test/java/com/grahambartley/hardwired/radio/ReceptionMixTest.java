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

  @ParameterizedTest(name = "voice on air {1}")
  @CsvSource({"0.8,true,0.24", "0.8,false,0.8", "0,true,0"})
  void aDiscDipsWhileAVoiceIsOnTheAir(double loudness, boolean voice, double expected) {
    assertEquals(expected, ReceptionMix.dippedForVoice(loudness, voice), 1e-9);
  }

  @ParameterizedTest(name = "volume {0} noise {1}")
  @CsvSource({"100,100,0.25", "50,100,0.125", "100,50,0.125", "0,100,0", "100,0,0"})
  void anEmptyDialHissesByTheKnobAndTheNoiseSetting(int volume, int noise, double expected) {
    assertEquals(expected, ReceptionMix.emptyDialHiss(volume, noise), 1e-9);
  }

  @ParameterizedTest(name = "strength {0}")
  @CsvSource({"1,0", "0.5,0.3", "0,0.6"})
  void hissRisesAsSignalFalls(double strength, double expected) {
    assertEquals(expected, ReceptionMix.stationHiss(100, 100, strength), 1e-9);
  }

  @ParameterizedTest(name = "strength {0}")
  @CsvSource({"1,0.12", "0,0.35"})
  void crackleIsLouderOnAWeakSignal(double strength, double expected) {
    assertEquals(expected, ReceptionMix.crackle(100, 100, strength), 1e-9);
  }

  @ParameterizedTest(name = "volume {0} noise {1}")
  @CsvSource({"100,100,0.5", "50,50,0.125", "100,0,0", "0,100,0"})
  void aSweepFollowsTheKnobAndTheNoiseSetting(int volume, int noise, double expected) {
    assertEquals(expected, ReceptionMix.sweep(volume, noise), 1e-9);
  }
}
