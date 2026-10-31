package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PartTypeTest {

  @ParameterizedTest(name = "{0}({1}, {2}) = {3}")
  @CsvSource({
    "AND, false, false, false",
    "AND, true, false, false",
    "AND, false, true, false",
    "AND, true, true, true",
    "OR, false, false, false",
    "OR, true, false, true",
    "OR, false, true, true",
    "OR, true, true, true",
    "NOT, false, false, true",
    "NOT, true, false, false",
    "XOR, false, false, false",
    "XOR, true, false, true",
    "XOR, false, true, true",
    "XOR, true, true, false",
    "NAND, false, false, true",
    "NAND, true, false, true",
    "NAND, false, true, true",
    "NAND, true, true, false",
    "NOR, false, false, true",
    "NOR, true, false, false",
    "NOR, false, true, false",
    "NOR, true, true, false"
  })
  void gatesFollowTheirTruthTables(PartType type, boolean a, boolean b, boolean expected) {
    assertEquals(expected, type.gate(a, b));
  }

  @ParameterizedTest(name = "{0} holds state, so it has no truth table")
  @EnumSource(
      value = PartType.class,
      names = {"SR_LATCH", "D_LATCH", "TIMER", "COUNTER"})
  void delayPartsAreNotGates(PartType type) {
    assertTrue(type.isDelay());
    assertThrows(IllegalStateException.class, () -> type.gate(true, true));
  }

  @ParameterizedTest(name = "{0} round trips through its id")
  @EnumSource(PartType.class)
  void idsRoundTrip(PartType type) {
    assertEquals(type, PartType.byId(type.id()));
  }

  @Test
  void anUnknownIdIsNoType() {
    assertNull(PartType.byId("flipflop"));
  }

  @ParameterizedTest(name = "{0} starts inside its own range")
  @EnumSource(PartType.class)
  void defaultSettingsAreInRange(PartType type) {
    assertTrue(type.defaultSetting() >= type.minSetting());
    assertTrue(type.defaultSetting() <= type.maxSetting());
  }

  @ParameterizedTest(name = "{0} has no setting")
  @EnumSource(
      value = PartType.class,
      names = {"AND", "OR", "NOT", "XOR", "NAND", "NOR", "SR_LATCH", "D_LATCH"})
  void onlyTimersAndCountersHaveASetting(PartType type) {
    assertFalse(type.hasSetting());
  }
}
