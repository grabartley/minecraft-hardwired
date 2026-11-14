package com.grahambartley.hardwired.radio;

public final class VoiceFormat {

  public static final int SAMPLE_RATE = 16_000;
  public static final int FRAME_SAMPLES = SAMPLE_RATE / 10;
  public static final int BYTES_PER_SAMPLE = 2;
  private static final int BYTE_BITS = 8;
  private static final int BYTE_MASK = 0xFF;

  private VoiceFormat() {}

  public static boolean frameFits(int frameBytes) {
    return frameBytes > AdpcmCodec.HEADER_BYTES
        && frameBytes <= AdpcmCodec.frameBytes(FRAME_SAMPLES);
  }

  public static int millisToBytes(int millis) {
    return SAMPLE_RATE * millis / 1000 * BYTES_PER_SAMPLE;
  }

  public static byte[] toBytes(short[] samples) {
    byte[] bytes = new byte[samples.length * BYTES_PER_SAMPLE];
    for (int i = 0; i < samples.length; i++) {
      bytes[BYTES_PER_SAMPLE * i] = (byte) samples[i];
      bytes[BYTES_PER_SAMPLE * i + 1] = (byte) (samples[i] >> BYTE_BITS);
    }
    return bytes;
  }

  public static short[] toSamples(byte[] bytes, int length) {
    short[] samples = new short[length / BYTES_PER_SAMPLE];
    for (int i = 0; i < samples.length; i++) {
      int low = bytes[BYTES_PER_SAMPLE * i] & BYTE_MASK;
      samples[i] = (short) (bytes[BYTES_PER_SAMPLE * i + 1] << BYTE_BITS | low);
    }
    return samples;
  }
}
