package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PanelPagesTest {

  @ParameterizedTest(name = "{0} rows: {1} pages")
  @CsvSource({"0, 1", "1, 1", "7, 1", "8, 2", "14, 2", "15, 3"})
  void pagesCoverEveryRow(int rows, int pages) {
    assertEquals(pages, PanelPages.pageCount(rows));
  }

  @ParameterizedTest(name = "page {0} of {1} rows: {2}")
  @CsvSource({"-1, 10, 0", "0, 10, 0", "1, 10, 1", "5, 10, 1", "3, 0, 0"})
  void aPageStaysInRange(int page, int rows, int expected) {
    assertEquals(expected, PanelPages.clamp(page, rows));
  }

  @ParameterizedTest(name = "page {0} of {1} rows: {2} to {3}")
  @CsvSource({"0, 3, 0, 3", "0, 20, 0, 7", "1, 20, 7, 14", "2, 20, 14, 20"})
  void aPageShowsItsRows(int page, int rows, int first, int last) {
    assertEquals(first, PanelPages.firstRow(page));
    assertEquals(last, PanelPages.lastRow(page, rows));
  }
}
