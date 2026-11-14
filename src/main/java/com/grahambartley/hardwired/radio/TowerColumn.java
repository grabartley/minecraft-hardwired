package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.block.BlockColumn;
import com.grahambartley.hardwired.config.RadioSettings;
import java.util.function.IntFunction;

public record TowerColumn(int footY, int masts, boolean capped) {

  public enum Cell {
    MAST,
    ROD,
    OTHER
  }

  public static TowerColumn read(int y, int minY, int maxY, IntFunction<Cell> cellAt) {
    BlockColumn column = BlockColumn.around(y, minY, maxY, at -> cellAt.apply(at) == Cell.MAST);
    boolean capped = column.top() < maxY && cellAt.apply(column.top() + 1) == Cell.ROD;
    return new TowerColumn(column.bottom(), column.height(), capped);
  }

  public boolean tallEnough(RadioSettings settings) {
    return masts >= settings.towerMinMasts();
  }

  public boolean stands(RadioSettings settings) {
    return capped && tallEnough(settings);
  }

  public int reach(RadioSettings settings) {
    return settings.towerBaseReach()
        + settings.towerReachPerMast() * Math.min(masts, settings.towerMaxMasts());
  }
}
