package com.grahambartley.hardwired.screen;

public final class PanelPages {

  public static final int ROWS_PER_PAGE = 7;

  private PanelPages() {}

  public static int pageCount(int rows) {
    return Math.max(1, (rows + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
  }

  public static int clamp(int page, int rows) {
    return Math.clamp(page, 0, pageCount(rows) - 1);
  }

  public static int firstRow(int page) {
    return page * ROWS_PER_PAGE;
  }

  public static int lastRow(int page, int rows) {
    return Math.min(rows, firstRow(page) + ROWS_PER_PAGE);
  }
}
