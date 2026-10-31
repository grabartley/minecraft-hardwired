package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DialFaceTest {

  private static DialFace.Pixel tip(int value, int max) {
    List<DialFace.Pixel> needle = DialFace.needle(value, max, 9);
    return needle.get(needle.size() - 1);
  }

  @ParameterizedTest(name = "value {0} of {1}")
  @CsvSource({"0, 15", "-3, 15", "0, 0"})
  void theNeedleRestsBottomLeftAtZero(int value, int max) {
    assertTrue(tip(value, max).x() < 0 && tip(value, max).y() > 0);
  }

  @ParameterizedTest(name = "value {0} of {1}")
  @CsvSource({"15, 15", "99, 15"})
  void theNeedleEndsBottomRightAtFull(int value, int max) {
    assertTrue(tip(value, max).x() > 0 && tip(value, max).y() > 0);
  }

  @ParameterizedTest(name = "radius {0}")
  @ValueSource(ints = {5, 7, 9, 12})
  void theRingStaysOnItsRadius(int radius) {
    for (DialFace.Pixel pixel : DialFace.ring(radius)) {
      assertEquals(radius, Math.hypot(pixel.x(), pixel.y()), 1.0);
    }
  }

  @ParameterizedTest(name = "half of {0}")
  @ValueSource(ints = {2, 14})
  void theNeedlePointsUpAtHalfway(int max) {
    DialFace.Pixel tip = tip(max / 2, max);
    assertTrue(tip.y() < 0 && Math.abs(tip.x()) <= 1);
  }
}
