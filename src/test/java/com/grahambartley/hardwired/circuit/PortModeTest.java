package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PortModeTest {

  @ParameterizedTest(name = "{0} round trips through its code")
  @EnumSource(PortMode.class)
  void codesRoundTrip(PortMode mode) {
    assertEquals(mode, PortMode.byCode(mode.code()));
  }

  @Test
  void anUnknownCodeIsNoMode() {
    assertNull(PortMode.byCode('x'));
  }

  @ParameterizedTest(name = "{0} is followed by {1}")
  @CsvSource({"UNUSED, INPUT", "INPUT, OUTPUT", "OUTPUT, UNUSED"})
  void modesCycle(PortMode mode, PortMode next) {
    assertEquals(next, mode.next());
  }
}
