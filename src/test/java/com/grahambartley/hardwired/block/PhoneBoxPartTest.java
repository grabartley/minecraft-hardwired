package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneBoxPartTest {

  @ParameterizedTest(name = "{0} sits {1} above the floor as {2}")
  @CsvSource({"LOWER,0,lower", "MIDDLE,1,middle", "UPPER,2,upper"})
  void partsStackFromTheFloor(PhoneBoxPart part, int height, String id) {
    assertEquals(height, part.height());
    assertEquals(id, part.asString());
  }
}
