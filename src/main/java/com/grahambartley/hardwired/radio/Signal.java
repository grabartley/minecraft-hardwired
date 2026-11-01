package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.config.RadioSettings;

public final class Signal {

  public static final int NEEDLE_STEPS = 5;
  private static final double ONE_STEP_OFF = 0.5;

  private Signal() {}

  public static double byDistance(double flatDistance, int reach) {
    return reach <= 0 ? 0 : Math.clamp(1 - flatDistance / reach, 0, 1);
  }

  public static double byDepth(int depth, int freeDepth, int lossDepth) {
    return Math.clamp(1 - (depth - freeDepth) / (double) lossDepth, 0, 1);
  }

  public static double byTuning(int stepsOff) {
    return switch (stepsOff) {
      case 0 -> 1;
      case 1 -> ONE_STEP_OFF;
      default -> 0;
    };
  }

  public static double strength(
      double flatDistance, int reach, int depth, int stepsOff, RadioSettings settings) {
    return byDistance(flatDistance, reach)
        * byDepth(depth, settings.undergroundFreeDepth(), settings.undergroundLossDepth())
        * byTuning(stepsOff);
  }

  public static int needle(double strength) {
    return (int) Math.ceil(Math.clamp(strength, 0, 1) * NEEDLE_STEPS);
  }
}
