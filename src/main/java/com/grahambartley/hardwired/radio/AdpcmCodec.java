package com.grahambartley.hardwired.radio;

public final class AdpcmCodec {

  public static final int HEADER_BYTES = 3;
  private static final int LOW_BYTE = 0;
  private static final int HIGH_BYTE = 1;
  private static final int INDEX_BYTE = 2;
  private static final int BYTE_BITS = 8;
  private static final int BYTE_MASK = 0xFF;
  private static final int NIBBLE_BITS = 4;
  private static final int NIBBLE_MASK = 0xF;
  private static final int MAGNITUDE_MASK = 0x7;
  private static final int SIGN_BIT = 0x8;
  private static final int PADDED_FLAG = 0x80;
  private static final int INDEX_MASK = 0x7F;
  private static final int[] STEPS = {
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73,
    80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494,
    544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499,
    2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442,
    11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
  };
  private static final int[] INDEX_MOVES = {-1, -1, -1, -1, 2, 4, 6, 8};

  private int stepIndex;

  public static int frameBytes(int samples) {
    return HEADER_BYTES + samples / 2;
  }

  public byte[] encode(short[] pcm) {
    if (pcm.length == 0) {
      return new byte[0];
    }
    byte[] frame = new byte[frameBytes(pcm.length)];
    int predictor = pcm[0];
    boolean padded = pcm.length % 2 == 0;
    frame[LOW_BYTE] = (byte) predictor;
    frame[HIGH_BYTE] = (byte) (predictor >> BYTE_BITS);
    frame[INDEX_BYTE] = (byte) (stepIndex | (padded ? PADDED_FLAG : 0));
    for (int i = 1; i < pcm.length; i++) {
      int step = STEPS[stepIndex];
      int code = codeFor(pcm[i] - predictor, step);
      predictor = next(predictor, step, code);
      stepIndex = nextIndex(stepIndex, code);
      int body = HEADER_BYTES + (i - 1) / 2;
      frame[body] |= (byte) (i % 2 == 1 ? code : code << NIBBLE_BITS);
    }
    return frame;
  }

  public static short[] decode(byte[] frame) {
    if (frame.length < HEADER_BYTES) {
      return new short[0];
    }
    int predictor = (short) (frame[HIGH_BYTE] << BYTE_BITS | frame[LOW_BYTE] & BYTE_MASK);
    int flags = frame[INDEX_BYTE] & BYTE_MASK;
    int index = Math.min(flags & INDEX_MASK, STEPS.length - 1);
    int nibbles = 2 * (frame.length - HEADER_BYTES) - ((flags & PADDED_FLAG) == 0 ? 0 : 1);
    short[] pcm = new short[1 + Math.max(nibbles, 0)];
    pcm[0] = (short) predictor;
    for (int i = 1; i < pcm.length; i++) {
      int packed = frame[HEADER_BYTES + (i - 1) / 2] & BYTE_MASK;
      int code = (i % 2 == 1 ? packed : packed >> NIBBLE_BITS) & NIBBLE_MASK;
      predictor = next(predictor, STEPS[index], code);
      index = nextIndex(index, code);
      pcm[i] = (short) predictor;
    }
    return pcm;
  }

  private static int codeFor(int difference, int step) {
    int code = difference < 0 ? SIGN_BIT : 0;
    int left = Math.abs(difference);
    int bit = SIGN_BIT >> 1;
    int part = step;
    while (bit != 0) {
      if (left >= part) {
        code |= bit;
        left -= part;
      }
      part >>= 1;
      bit >>= 1;
    }
    return code;
  }

  private static int next(int predictor, int step, int code) {
    int change = step >> 3;
    if ((code & 4) != 0) {
      change += step;
    }
    if ((code & 2) != 0) {
      change += step >> 1;
    }
    if ((code & 1) != 0) {
      change += step >> 2;
    }
    int signed = (code & SIGN_BIT) != 0 ? predictor - change : predictor + change;
    return Math.clamp(signed, Short.MIN_VALUE, Short.MAX_VALUE);
  }

  private static int nextIndex(int index, int code) {
    return Math.clamp(index + INDEX_MOVES[code & MAGNITUDE_MASK], 0, STEPS.length - 1);
  }
}
