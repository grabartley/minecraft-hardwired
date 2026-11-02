package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LiftGateSwingTest {

  private static final float CLOSE = 1.0E-4F;

  @ParameterizedTest(name = "open {0}")
  @ValueSource(booleans = {true, false})
  void aGateFirstSeenDoesNotAnimate(boolean open) {
    LiftGateSwing swing = new LiftGateSwing();
    float settled = new LiftGateSwing().width(open, 1000);
    assertEquals(settled, swing.width(open, 5000), CLOSE);
    assertEquals(open ? settled : 1F, swing.width(open, 5001), CLOSE);
  }

  @Test
  void openingFoldsTheGateOverTime() {
    LiftGateSwing swing = new LiftGateSwing();
    swing.width(false, 0);
    float start = swing.width(true, 100);
    float middle = swing.width(true, 100 + LiftGateSwing.TICKS / 2.0);
    float end = swing.width(true, 100 + LiftGateSwing.TICKS);
    assertEquals(1F, start, CLOSE);
    assertTrue(middle < start && middle > end);
    assertEquals(end, swing.width(true, 900), CLOSE);
  }

  @Test
  void closingSpreadsItAgain() {
    LiftGateSwing swing = new LiftGateSwing();
    swing.width(true, 0);
    float folded = swing.width(false, 100);
    assertTrue(folded < 1F);
    assertEquals(1F, swing.width(false, 100 + LiftGateSwing.TICKS), CLOSE);
  }
}
