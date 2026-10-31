package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class TimelineGridTest {

  private static final TimelineGrid GRID = new TimelineGrid(100, 50, 14);

  private static Stream<Arguments> clicks() {
    return Stream.of(
        Arguments.of(100.0, 50.0, 0, 20, Optional.of(new TimelineGrid.Cell(0, 0))),
        Arguments.of(113.9, 63.9, 0, 20, Optional.of(new TimelineGrid.Cell(0, 0))),
        Arguments.of(114.0, 64.0, 0, 20, Optional.of(new TimelineGrid.Cell(1, 1))),
        Arguments.of(100.0, 50.0, 16, 20, Optional.of(new TimelineGrid.Cell(16, 0))),
        Arguments.of(99.0, 50.0, 0, 20, Optional.empty()),
        Arguments.of(100.0, 49.0, 0, 20, Optional.empty()),
        Arguments.of(100.0, 162.0, 0, 20, Optional.empty()),
        Arguments.of(128.0, 50.0, 0, 2, Optional.empty()),
        Arguments.of(324.0, 50.0, 0, 32, Optional.empty()));
  }

  @ParameterizedTest(name = "({0}, {1}) from step {2}")
  @MethodSource("clicks")
  void clicksLandOnTheirCell(
      double x, double y, int first, int steps, Optional<TimelineGrid.Cell> expected) {
    assertEquals(expected, GRID.cellAt(x, y, first, steps));
  }

  @Test
  void aHeaderClickPicksItsStep() {
    assertEquals(OptionalInt.of(1), GRID.headerAt(115, 40, 0, 3));
    assertEquals(OptionalInt.empty(), GRID.headerAt(115, 50, 0, 3));
  }

  @Test
  void aLabelClickPicksItsSlot() {
    assertEquals(OptionalInt.of(2), GRID.labelAt(GRID.labelX() + 1, 80));
    assertEquals(OptionalInt.empty(), GRID.labelAt(GRID.labelX() + 1, 170));
  }

  @ParameterizedTest(name = "step {0} is on the page from {1}")
  @CsvSource({"-1, 0", "0, 0", "15, 0", "16, 16", "31, 16"})
  void pagesStartOnWholeWidths(int step, int start) {
    assertEquals(start, TimelineGrid.pageStart(step));
  }
}
