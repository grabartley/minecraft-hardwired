package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.config.RadioSettings;
import java.util.function.IntFunction;

public record TowerColumn(int footY, int masts, boolean capped) {

  public enum Cell {
    MAST,
    ROD,
    OTHER
  }

  public static TowerColumn read(int y, int minY, int maxY, IntFunction<Cell> cellAt) {
    int foot = y;
    while (foot > minY && cellAt.apply(foot - 1) == Cell.MAST) {
      foot--;
    }
    int top = y;
    while (top < maxY && cellAt.apply(top + 1) == Cell.MAST) {
      top++;
    }
    boolean capped = top < maxY && cellAt.apply(top + 1) == Cell.ROD;
    return new TowerColumn(foot, top - foot + 1, capped);
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
