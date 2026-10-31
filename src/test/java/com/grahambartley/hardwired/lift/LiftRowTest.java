package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftRowTest {

  private static final List<Integer> ORDERED = List.of(10, 14, 20);

  @Test
  void theTopFloorIsListedFirstAndNumbersCountFromTheBottom() {
    assertEquals(
        List.of(new LiftRow(3, 20), new LiftRow(2, 14), new LiftRow(1, 10)),
        LiftRow.topDown(ORDERED));
  }

  @ParameterizedTest(name = "level {0} with {1} per page")
  @CsvSource({"20,1,0", "14,1,1", "10,1,2", "10,2,1", "99,2,0"})
  void theScreenOpensOnThePageOfThisFloor(int level, int perPage, int page) {
    assertEquals(page, LiftRow.pageOf(LiftRow.topDown(ORDERED), level, perPage));
  }
}
