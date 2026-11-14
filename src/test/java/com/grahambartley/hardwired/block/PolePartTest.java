package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolePartTest {

  @ParameterizedTest(name = "below {0} above {1}")
  @CsvSource({
    "false,false,SINGLE,true",
    "false,true,FOOT,false",
    "true,true,MIDDLE,false",
    "true,false,TOP,true"
  })
  void aPoleBlockFitsItselfToItsColumnAndOnlyTheTopHoldsSpans(
      boolean below, boolean above, PolePart part, boolean holds) {
    assertEquals(part, PolePart.between(below, above));
    assertEquals(holds, part.holdsSpans());
  }
}
