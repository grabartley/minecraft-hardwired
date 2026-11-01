package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdpcmCodecTest {

  private static final int RATE = VoiceFormat.SAMPLE_RATE;
  private static final int WARMUP_FRAMES = 2;

  private static short[] tone(double hertz, int samples, int start, double level) {
    short[] pcm = new short[samples];
    for (int i = 0; i < samples; i++) {
      pcm[i] = (short) (level * Math.sin(2 * Math.PI * hertz * (start + i) / RATE));
    }
    return pcm;
  }

  private static double signalToNoiseDb(short[] original, short[] decoded) {
    double signal = 0;
    double noise = 0;
    for (int i = 0; i < original.length; i++) {
      signal += original[i] * (double) original[i];
      double difference = original[i] - (double) decoded[i];
      noise += difference * difference;
    }
    return 10 * Math.log10(signal / Math.max(noise, 1));
  }

  @ParameterizedTest(name = "{0} samples")
  @ValueSource(ints = {1, 2, 3, 4, 159, 160, 1600, 1601})
  void everyLengthRoundTrips(int samples) {
    short[] pcm = tone(440, samples, 0, 8000);
    byte[] frame = new AdpcmCodec().encode(pcm);
    assertEquals(AdpcmCodec.frameBytes(samples), frame.length);
    assertEquals(samples, AdpcmCodec.decode(frame).length);
    assertEquals(pcm[0], AdpcmCodec.decode(frame)[0]);
  }

  @ParameterizedTest(name = "{0} Hz at level {1}")
  @CsvSource({"300,12000,20", "440,8000,20", "1000,8000,20", "2000,20000,15", "440,500,15"})
  void aToneComesBackCleanOnceTheStepHasSettled(double hertz, double level, double leastDb) {
    AdpcmCodec codec = new AdpcmCodec();
    int samples = VoiceFormat.FRAME_SAMPLES;
    short[] kept = null;
    short[] decoded = null;
    for (int frame = 0; frame <= WARMUP_FRAMES; frame++) {
      kept = tone(hertz, samples, frame * samples, level);
      decoded = AdpcmCodec.decode(codec.encode(kept));
    }
    assertTrue(signalToNoiseDb(kept, decoded) >= leastDb);
  }

  @ParameterizedTest(name = "seed {0}")
  @ValueSource(longs = {1, 2, 3})
  void noBytesFromTheWireCanBreakTheDecoder(long seed) {
    Random random = new Random(seed);
    for (int length = 0; length < 900; length += 7) {
      byte[] garbage = new byte[length];
      random.nextBytes(garbage);
      int expected = length < AdpcmCodec.HEADER_BYTES ? 0 : 1;
      assertTrue(AdpcmCodec.decode(garbage).length >= expected);
    }
  }

  @ParameterizedTest(name = "frame of {0} samples is {1} bytes")
  @CsvSource({"1600,803", "160,83"})
  void aFrameCostsFourBitsASampleAndAHeader(int samples, int bytes) {
    assertEquals(bytes, AdpcmCodec.frameBytes(samples));
  }

  @ParameterizedTest(name = "empty input {0}")
  @ValueSource(ints = {0})
  void nothingEncodesToNothing(int samples) {
    assertArrayEquals(new byte[0], new AdpcmCodec().encode(new short[samples]));
  }

  @ParameterizedTest(name = "extreme level {0}")
  @ValueSource(shorts = {Short.MAX_VALUE, Short.MIN_VALUE})
  void fullScaleDoesNotWrapAround(short level) {
    short[] pcm = new short[400];
    java.util.Arrays.fill(pcm, level);
    for (short sample : AdpcmCodec.decode(new AdpcmCodec().encode(pcm))) {
      assertEquals(Math.signum(level), Math.signum(sample));
    }
  }
}
