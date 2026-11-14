package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlockColumnTest {

  private static BlockColumn read(String column, int y) {
    return BlockColumn.around(y, 0, column.length() - 1, at -> column.charAt(at) == 'P');
  }

  @ParameterizedTest(name = "[{0}] from {1}")
  @CsvSource({"PPPP,0,0,3", "PPPP,2,0,3", ".PP.,1,1,2", "P.PPP,3,2,4", "P.PPP,0,0,0", ".P..,1,1,1"})
  void aColumnRunsFromItsLowestToItsHighestTouchingMember(
      String column, int y, int bottom, int top) {
    assertEquals(new BlockColumn(bottom, top), read(column, y));
    assertEquals(top - bottom + 1, read(column, y).height());
  }
}
