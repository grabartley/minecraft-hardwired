package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PartTest {

  @ParameterizedTest(name = "a new {0} takes the default setting")
  @EnumSource(PartType.class)
  void aNewPartTakesTheDefaultSetting(PartType type) {
    assertEquals(type.defaultSetting(), Part.of(type, 1, 2).setting());
  }

  @ParameterizedTest(name = "moving a {0} keeps its setting")
  @EnumSource(PartType.class)
  void movingKeepsTheSetting(PartType type) {
    assertEquals(new Part(type, 7, 8, 3), new Part(type, 1, 2, 3).movedTo(7, 8));
  }

  @ParameterizedTest(name = "a new setting on a {0} keeps its cell")
  @EnumSource(PartType.class)
  void aNewSettingKeepsTheCell(PartType type) {
    assertEquals(new Part(type, 1, 2, 9), new Part(type, 1, 2, 3).withSetting(9));
  }
}
