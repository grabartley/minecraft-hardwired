package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExchangePartTest {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "LOWER_LEFT,lower_left,false,false",
    "LOWER_RIGHT,lower_right,true,false",
    "UPPER_LEFT,upper_left,false,true",
    "UPPER_RIGHT,upper_right,true,true"
  })
  void eachPartKnowsItsCornerAndStateName(
      ExchangePart part, String name, boolean right, boolean upper) {
    assertEquals(name, part.asString());
    assertEquals(right, part.right());
    assertEquals(upper, part.upper());
  }
}
