package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PanelActionTest {

  @ParameterizedTest(name = "{0} is {1}")
  @CsvSource({"0, REFRESH", "1, TOGGLE", "2, SET_LOOK", "3, REFRESH", "-1, SET_LOOK"})
  void anyIndexMapsToAnAction(int index, PanelAction action) {
    assertEquals(action, PanelAction.byIndex(index));
  }
}
