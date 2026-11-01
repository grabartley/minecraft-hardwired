package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceJitterTest {

  private static byte[] filled(int length, int value) {
    byte[] bytes = new byte[length];
    java.util.Arrays.fill(bytes, (byte) value);
    return bytes;
  }

  @ParameterizedTest(name = "take {0}")
  @ValueSource(ints = {1, 8, 100})
  void emptyGivesSilenceOfTheAskedLength(int length) {
    assertArrayEquals(new byte[length], new VoiceJitter(4, 16).take(length));
  }

  @ParameterizedTest(name = "holding {0} of prefill 8")
  @ValueSource(ints = {1, 7})
  void nothingFlowsUntilThePrefillIsHeld(int held) {
    VoiceJitter jitter = new VoiceJitter(8, 32);
    jitter.offer(filled(held, 9));
    assertArrayEquals(new byte[4], jitter.take(4));
    assertEquals(held, jitter.held());
  }

  @ParameterizedTest(name = "take {0}")
  @ValueSource(ints = {2, 4, 8})
  void onceFlowingBytesComeOutInOrder(int length) {
    VoiceJitter jitter = new VoiceJitter(8, 32);
    jitter.offer(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    byte[] out = jitter.take(length);
    for (int i = 0; i < length; i++) {
      assertEquals(i + 1, out[i]);
    }
  }

  @ParameterizedTest(name = "short by {0}")
  @ValueSource(ints = {1, 3})
  void runningShortPadsWithSilenceAndPrefillsAgain(int shortBy) {
    VoiceJitter jitter = new VoiceJitter(4, 32);
    jitter.offer(filled(4, 7));
    byte[] out = jitter.take(4 + shortBy);
    assertEquals(7, out[3]);
    assertEquals(0, out[4]);
    jitter.offer(filled(2, 5));
    assertArrayEquals(new byte[2], jitter.take(2));
  }

  @ParameterizedTest(name = "offered {0} into 8")
  @CsvSource({"12,4,8", "8,0,8", "20,12,8"})
  void overflowDropsTheOldestSoDelayStaysBounded(int offered, int firstKept, int held) {
    VoiceJitter jitter = new VoiceJitter(1, 8);
    byte[] pcm = new byte[offered];
    for (int i = 0; i < offered; i++) {
      pcm[i] = (byte) i;
    }
    jitter.offer(pcm);
    assertEquals(held, jitter.held());
    assertEquals(firstKept, jitter.take(1)[0]);
  }
}
