package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PanelTextTest {

  @ParameterizedTest(name = "{0} wide in {1} overflows by {2}")
  @CsvSource({"100,216,0", "216,216,0", "217,216,1", "400,216,184"})
  void onlyTextWiderThanItsSpaceOverflows(int textWidth, int space, int overflow) {
    assertEquals(overflow, PanelText.overflow(textWidth, space));
  }

  @ParameterizedTest(name = "{3} starts at {4}")
  @CsvSource({
    "10,110,40,LEFT,10",
    "10,110,40,CENTER,40",
    "10,110,40,RIGHT,70",
    "10,110,100,CENTER,10",
    "10,110,100,RIGHT,10"
  })
  void textThatFitsSitsWhereItIsAligned(
      int left, int right, int textWidth, PanelText.Align align, int x) {
    assertEquals(x, PanelText.startX(left, right, textWidth, align));
  }

  @ParameterizedTest(name = "overflow {0}")
  @ValueSource(ints = {1, 40, 184, 1000})
  void scrollingStartsAtTheHeadAndReachesTheTail(int overflow) {
    assertEquals(0, PanelText.scrollOffset(overflow, 0));
    int furthest = 0;
    for (long millis = 0; millis < 120_000; millis += 16) {
      int offset = PanelText.scrollOffset(overflow, millis);
      assertTrue(offset >= 0 && offset <= overflow, "offset " + offset);
      furthest = Math.max(furthest, offset);
    }
    assertEquals(overflow, furthest);
  }

  @ParameterizedTest(name = "overflow {0}")
  @ValueSource(ints = {10, 184})
  void scrollingNeverJumps(int overflow) {
    for (long millis = 0; millis < 60_000; millis += 16) {
      int step =
          Math.abs(
              PanelText.scrollOffset(overflow, millis + 16)
                  - PanelText.scrollOffset(overflow, millis));
      assertTrue(step <= 2, "jumped " + step + " at " + millis);
    }
  }
}
