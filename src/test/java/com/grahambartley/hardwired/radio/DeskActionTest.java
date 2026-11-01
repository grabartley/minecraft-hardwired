package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeskActionTest {

  @ParameterizedTest(name = "{0} is {1}")
  @CsvSource({
    "0,SET_FREQUENCY",
    "4,EJECT",
    "5,TAKE_MIC",
    "6,RELEASE_MIC",
    "7,SET_FREQUENCY",
    "-1,RELEASE_MIC"
  })
  void anyIndexFromAClientLandsOnAnAction(int index, DeskAction expected) {
    assertEquals(expected, DeskAction.byIndex(index));
  }
}
