package com.grahambartley.hardwired.radio;

public final class ReceptionMix {

  public static final int MAX_VOLUME = 100;
  public static final int MAX_NOISE = 100;
  private static final double WEAKEST_STATION = 0.35;
  private static final double VOICE_DIP = 0.3;
  private static final double EMPTY_DIAL_HISS = 0.25;
  private static final double WEAK_SIGNAL_HISS = 0.6;
  private static final double FAINTEST_CRACKLE = 0.25;
  private static final double LOUDEST_CRACKLE = 0.35;
  private static final double SWEEP = 0.5;

  private ReceptionMix() {}

  public static double stationLoudness(int volume, double strength) {
    return byKnob(volume) * (WEAKEST_STATION + (1 - WEAKEST_STATION) * signal(strength));
  }

  public static double dippedForVoice(double loudness, boolean voiceOnAir) {
    return voiceOnAir ? loudness * VOICE_DIP : loudness;
  }

  public static double emptyDialHiss(int volume, int noise) {
    return byKnob(volume) * byNoise(noise) * EMPTY_DIAL_HISS;
  }

  public static double stationHiss(int volume, int noise, double strength) {
    return byKnob(volume) * byNoise(noise) * WEAK_SIGNAL_HISS * (1 - signal(strength));
  }

  public static double crackle(int volume, int noise, double strength) {
    double weakness = 1 - signal(strength);
    return byKnob(volume)
        * byNoise(noise)
        * (FAINTEST_CRACKLE + (LOUDEST_CRACKLE - FAINTEST_CRACKLE) * weakness);
  }

  public static double sweep(int volume, int noise) {
    return byKnob(volume) * byNoise(noise) * SWEEP;
  }

  private static double byKnob(int volume) {
    return Math.clamp(volume, 0, MAX_VOLUME) / (double) MAX_VOLUME;
  }

  private static double byNoise(int noise) {
    return Math.clamp(noise, 0, MAX_NOISE) / (double) MAX_NOISE;
  }

  private static double signal(double strength) {
    return Math.clamp(strength, 0, 1);
  }
}
