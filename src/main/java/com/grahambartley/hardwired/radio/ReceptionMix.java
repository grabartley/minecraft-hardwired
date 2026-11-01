package com.grahambartley.hardwired.radio;

public final class ReceptionMix {

  public static final int MAX_VOLUME = 100;
  private static final double WEAKEST_STATION = 0.35;

  private ReceptionMix() {}

  public static double stationLoudness(int volume, double strength) {
    double byKnob = Math.clamp(volume, 0, MAX_VOLUME) / (double) MAX_VOLUME;
    return byKnob * (WEAKEST_STATION + (1 - WEAKEST_STATION) * Math.clamp(strength, 0, 1));
  }
}
