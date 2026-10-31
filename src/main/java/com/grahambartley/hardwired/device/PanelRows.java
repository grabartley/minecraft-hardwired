package com.grahambartley.hardwired.device;

import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public final class PanelRows {

  private PanelRows() {}

  public static List<PanelRow> ordered(List<PanelRow> rows, BlockPos panel) {
    return rows.stream()
        .sorted(
            Comparator.comparing(PanelRow::kind)
                .thenComparingDouble(row -> row.pos().getSquaredDistance(panel))
                .thenComparing(PanelRow::pos))
        .toList();
  }

  public static List<PanelRow> capped(List<PanelRow> rows) {
    return rows.subList(0, Math.min(rows.size(), PanelRow.MAX_ROWS));
  }

  public static int omitted(List<PanelRow> rows) {
    return Math.max(0, rows.size() - PanelRow.MAX_ROWS);
  }
}
