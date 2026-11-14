package com.grahambartley.hardwired.block;

import java.util.function.IntPredicate;

public record BlockColumn(int bottom, int top) {

  public static BlockColumn around(int y, int minY, int maxY, IntPredicate member) {
    int bottom = y;
    while (bottom > minY && member.test(bottom - 1)) {
      bottom--;
    }
    int top = y;
    while (top < maxY && member.test(top + 1)) {
      top++;
    }
    return new BlockColumn(bottom, top);
  }

  public int height() {
    return top - bottom + 1;
  }
}
