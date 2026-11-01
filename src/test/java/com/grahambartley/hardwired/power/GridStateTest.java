package com.grahambartley.hardwired.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.device.DeviceStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GridStateTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "NO_SOURCE,NO_SOURCE,false",
    "SOURCES_OFF,SOURCES_OFF,false",
    "OUT_OF_FUEL,OUT_OF_FUEL,false",
    "OVERLOADED,OVERLOADED,false",
    "RETURNING,POWER_RETURNING,false",
    "LIVE,RUNNING,true"
  })
  void everyStateHasAReason(GridState state, DeviceStatus reason, boolean power) {
    assertEquals(reason, state.stopReason());
    assertEquals(power, state.givesPower());
  }

  @ParameterizedTest(name = "index {0}")
  @CsvSource({"-1,NO_SOURCE", "5,LIVE", "99,LIVE"})
  void aBadSavedIndexStaysInRange(int index, GridState state) {
    assertEquals(state, GridState.byIndex(index));
  }
}
