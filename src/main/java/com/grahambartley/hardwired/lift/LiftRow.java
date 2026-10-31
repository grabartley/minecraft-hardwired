package com.grahambartley.hardwired.lift;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record LiftRow(int number, int level) {

  public static List<LiftRow> topDown(List<Integer> ordered) {
    List<LiftRow> rows = new ArrayList<>();
    for (int i = 0; i < ordered.size(); i++) {
      rows.add(new LiftRow(i + 1, ordered.get(i)));
    }
    rows.sort(Comparator.comparingInt(LiftRow::level).reversed());
    return List.copyOf(rows);
  }

  public static int pageOf(List<LiftRow> rows, int level, int rowsPerPage) {
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i).level() == level) {
        return i / rowsPerPage;
      }
    }
    return 0;
  }
}
