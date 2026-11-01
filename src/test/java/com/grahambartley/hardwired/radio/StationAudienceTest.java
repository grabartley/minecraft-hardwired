package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StationAudienceTest {

  private static final BlockPos ONE = new BlockPos(1, 2, 3);
  private static final BlockPos TWO = new BlockPos(4, 5, 6);

  @ParameterizedTest(name = "device {0}")
  @ValueSource(strings = {"radio", "speaker"})
  void aDeviceMovesBetweenStations(String device) {
    StationAudience<String> audience = new StationAudience<>();
    audience.retune(device, null, ONE);
    assertEquals(Set.of(device), audience.tunedTo(ONE));
    audience.retune(device, ONE, TWO);
    assertEquals(Set.of(), audience.tunedTo(ONE));
    assertEquals(Set.of(device), audience.tunedTo(TWO));
    audience.retune(device, TWO, null);
    assertEquals(Set.of(), audience.tunedTo(TWO));
  }

  @ParameterizedTest(name = "device {0}")
  @ValueSource(strings = {"radio"})
  void leavingOneDeviceKeepsTheOthers(String device) {
    StationAudience<String> audience = new StationAudience<>();
    audience.retune(device, null, ONE);
    audience.retune("other", null, ONE);
    audience.retune(device, ONE, null);
    assertEquals(Set.of("other"), audience.tunedTo(ONE));
  }
}
