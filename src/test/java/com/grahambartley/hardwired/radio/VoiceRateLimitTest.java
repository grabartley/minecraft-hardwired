package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceRateLimitTest {

  @ParameterizedTest(name = "steady every {0} ms")
  @ValueSource(ints = {100, 90, 84})
  void aRealMicrophoneIsNeverDropped(int everyMillis) {
    VoiceRateLimit limit = new VoiceRateLimit(0);
    for (int i = 1; i <= 600; i++) {
      assertTrue(limit.allows((long) i * everyMillis));
    }
  }

  @ParameterizedTest(name = "flood of {0} in one instant lets {1} through")
  @CsvSource({"100,6", "6,6", "3,3"})
  void aFloodOnlyGetsTheBurst(int flood, int passed) {
    VoiceRateLimit limit = new VoiceRateLimit(0);
    int allowed = 0;
    for (int i = 0; i < flood; i++) {
      allowed += limit.allows(0) ? 1 : 0;
    }
    assertEquals(passed, allowed);
  }

  @ParameterizedTest(name = "after {0} ms")
  @CsvSource({"84,1", "167,2", "1000,6"})
  void framesAreEarnedBackOverTime(int waitMillis, int frames) {
    VoiceRateLimit limit = new VoiceRateLimit(0);
    while (limit.allows(0)) {
      continue;
    }
    int allowed = 0;
    while (limit.allows(waitMillis)) {
      allowed++;
    }
    assertEquals(frames, allowed);
  }

  @ParameterizedTest(name = "a clock going back {0} ms")
  @ValueSource(longs = {1, 1000})
  void aClockGoingBackEarnsNothing(long back) {
    VoiceRateLimit limit = new VoiceRateLimit(10_000);
    while (limit.allows(10_000)) {
      continue;
    }
    assertFalse(limit.allows(10_000 - back));
  }
}
