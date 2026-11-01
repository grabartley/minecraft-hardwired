package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VoiceAudienceTest {

  @ParameterizedTest(name = "at {0},{1},{2} heard {3}")
  @CsvSource({"0,0,0,true", "24,0,0,true", "24.1,0,0,false", "0,0,24,true", "100,0,0,true"})
  void aPlayerHearsAnyDeviceInItsOwnHearing(double x, double y, double z, boolean heard) {
    List<VoiceAudience.Device> devices =
        List.of(new VoiceAudience.Device(0, 0, 0, 24), new VoiceAudience.Device(100, 0, 0, 64));
    assertEquals(heard, VoiceAudience.hears(x, y, z, devices));
  }

  @ParameterizedTest(name = "nobody at {0}")
  @CsvSource({"0", "1000"})
  void noDevicesMeansNoAudience(double x) {
    assertEquals(false, VoiceAudience.hears(x, 0, 0, List.of()));
  }
}
