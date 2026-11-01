package com.grahambartley.hardwired.radio;

public final class RadioTone {

  private static final double LOW_CUT_HZ = 200D;
  private static final double HIGH_CUT_HZ = 6000D;
  private static final double DRIVE = 1.4D;

  private final double lowCut;
  private final double highCut;
  private final double[] rumble;
  private final double[] firstPole;
  private final double[] secondPole;

  public RadioTone(float sampleRate, int channels) {
    lowCut = poleFor(LOW_CUT_HZ, sampleRate);
    highCut = poleFor(HIGH_CUT_HZ, sampleRate);
    rumble = new double[channels];
    firstPole = new double[channels];
    secondPole = new double[channels];
  }

  private static double poleFor(double hertz, float sampleRate) {
    return 1 - Math.exp(-2 * Math.PI * hertz / sampleRate);
  }

  public double shape(int channel, double sample) {
    rumble[channel] += lowCut * (sample - rumble[channel]);
    firstPole[channel] += highCut * (sample - rumble[channel] - firstPole[channel]);
    secondPole[channel] += highCut * (firstPole[channel] - secondPole[channel]);
    return Math.tanh(DRIVE * secondPole[channel]) / DRIVE;
  }
}
