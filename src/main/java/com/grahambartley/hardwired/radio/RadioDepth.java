package com.grahambartley.hardwired.radio;

import java.util.function.IntPredicate;

public final class RadioDepth {

  private RadioDepth() {}

  public static int below(int topY, int radioY, IntPredicate solidAt) {
    for (int y = topY - 1; y > radioY; y--) {
      if (solidAt.test(y)) {
        return y + 1 - radioY;
      }
    }
    return 0;
  }
}
