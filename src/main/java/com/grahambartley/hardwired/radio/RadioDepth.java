package com.grahambartley.hardwired.radio;

import java.util.function.IntToDoubleFunction;

public final class RadioDepth {

  private RadioDepth() {}

  public static double above(int topY, int radioY, IntToDoubleFunction layerAt) {
    double layers = 0;
    for (int y = topY - 1; y > radioY; y--) {
      layers += layerAt.applyAsDouble(y);
    }
    return layers;
  }
}
