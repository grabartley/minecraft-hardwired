package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DeskActionTest {

  @ParameterizedTest(name = "{0} is {1}")
  @CsvSource({"0,SET_FREQUENCY", "4,EJECT", "5,TAKE_MIC", "6,RELEASE_MIC", "7,RESTART"})
  void aValidIndexIsItsAction(int index, DeskAction expected) {
    assertEquals(expected, DeskAction.byIndex(index));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(ints = {-1, 8, Integer.MIN_VALUE, Integer.MAX_VALUE})
  void anIndexOutOfRangeIsNoAction(int index) {
    assertNull(DeskAction.byIndex(index));
  }
}
