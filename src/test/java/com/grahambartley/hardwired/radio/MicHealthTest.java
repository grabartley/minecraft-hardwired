package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MicHealthTest {

  @ParameterizedTest(name = "{0} dead frames refused {1}")
  @CsvSource({"9,false", "10,true", "11,true"})
  void aLineOfPureZeroesMeansTheSystemRefusedIt(int frames, boolean refused) {
    MicHealth health = new MicHealth();
    boolean last = false;
    for (int i = 0; i < frames; i++) {
      last = health.refused(new short[160]);
    }
    assertEquals(refused, last);
  }

  @ParameterizedTest(name = "one quiet sample of {0}")
  @CsvSource({"1", "-1"})
  void anyRealSampleResetsTheCount(short sample) {
    MicHealth health = new MicHealth();
    for (int i = 0; i < 9; i++) {
      health.refused(new short[160]);
    }
    health.refused(new short[] {0, sample});
    for (int i = 0; i < 9; i++) {
      assertEquals(false, health.refused(new short[160]));
    }
  }
}
