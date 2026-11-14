package com.grahambartley.hardwired.radio;

public final class RadioTone {

  public enum Band {
    RADIO(200D, 6000D),
    TELEPHONE(400D, 3000D);

    private final double lowCutHz;
    private final double highCutHz;

    Band(double lowCutHz, double highCutHz) {
      this.lowCutHz = lowCutHz;
      this.highCutHz = highCutHz;
    }
  }

  private static final double DRIVE = 1.4D;

  private final double lowCut;
  private final double highCut;
  private final double[] rumble;
  private final double[] firstPole;
  private final double[] secondPole;

  public RadioTone(float sampleRate, int channels, Band band) {
    lowCut = poleFor(band.lowCutHz, sampleRate);
    highCut = poleFor(band.highCutHz, sampleRate);
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
