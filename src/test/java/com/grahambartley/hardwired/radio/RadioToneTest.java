package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RadioToneTest {

  private static final int SECONDS = 2;
  private static final double QUIET = 0.1;

  private static double loudness(double[] signal, int from) {
    double sum = 0;
    for (int i = from; i < signal.length; i++) {
      sum += signal[i] * signal[i];
    }
    return Math.sqrt(sum / (signal.length - from));
  }

  private static double[] sine(double hertz, double level, int rate) {
    double[] signal = new double[SECONDS * rate];
    for (int i = 0; i < signal.length; i++) {
      signal[i] = level * Math.sin(2 * Math.PI * hertz * i / rate);
    }
    return signal;
  }

  private static double gainAt(double hertz, double level, int rate) {
    double[] in = sine(hertz, level, rate);
    double[] out = new double[in.length];
    RadioTone tone = new RadioTone(rate, 1, RadioTone.Band.RADIO);
    for (int i = 0; i < in.length; i++) {
      out[i] = tone.shape(0, in[i]);
    }
    return loudness(out, rate) / loudness(in, rate);
  }

  @ParameterizedTest(name = "{0} Hz at {1} Hz stays between {2} and {3}")
  @CsvSource({
    "1000,44100,0.85,1.01",
    "1000,48000,0.85,1.01",
    "1000,16000,0.85,1.01",
    "400,44100,0.85,1.01",
    "2500,44100,0.7,1.01"
  })
  void speechAndMelodyStayClear(double hertz, int rate, double least, double most) {
    double gain = gainAt(hertz, QUIET, rate);
    assertTrue(gain >= least && gain <= most, "gain " + gain);
  }

  @ParameterizedTest(name = "{0} Hz at {1} Hz drops below {2}")
  @CsvSource({"40,44100,0.25", "12000,44100,0.3", "16000,48000,0.2", "7000,16000,0.7"})
  void theVeryLowAndVeryHighFallAway(double hertz, int rate, double most) {
    double gain = gainAt(hertz, QUIET, rate);
    assertTrue(gain <= most, "gain " + gain);
  }

  @ParameterizedTest(name = "level {0}")
  @ValueSource(doubles = {0.5, 1})
  void loudPassagesSoftenAndNeverClip(double level) {
    RadioTone tone = new RadioTone(44100, 1, RadioTone.Band.RADIO);
    for (double sample : sine(1000, level, 44100)) {
      assertTrue(Math.abs(tone.shape(0, sample)) < 1);
    }
    assertTrue(gainAt(1000, level, 44100) < gainAt(1000, QUIET, 44100));
  }

  @ParameterizedTest(name = "{0} channels")
  @ValueSource(ints = {2, 6})
  void channelsDoNotBleed(int channels) {
    RadioTone tone = new RadioTone(44100, channels, RadioTone.Band.RADIO);
    for (double sample : sine(1000, 0.5, 44100)) {
      tone.shape(0, sample);
    }
    assertEquals(0D, tone.shape(1, 0D));
  }

  @ParameterizedTest(name = "{0} Hz")
  @CsvSource({"150,true", "5000,true", "1000,false"})
  void aTelephoneLineSoundsThinnerThanARadio(double hertz, boolean thinner) {
    double radio = bandGain(hertz, RadioTone.Band.RADIO);
    double telephone = bandGain(hertz, RadioTone.Band.TELEPHONE);
    assertEquals(thinner, telephone < radio * 0.8, "radio " + radio + " telephone " + telephone);
  }

  private static double bandGain(double hertz, RadioTone.Band band) {
    int rate = 16000;
    double[] in = sine(hertz, QUIET, rate);
    double[] out = new double[in.length];
    RadioTone tone = new RadioTone(rate, 1, band);
    for (int i = 0; i < in.length; i++) {
      out[i] = tone.shape(0, in[i]);
    }
    return loudness(out, rate) / loudness(in, rate);
  }
}
