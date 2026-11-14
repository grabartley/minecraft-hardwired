package com.grahambartley.hardwired.client.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanCurveTest {

  private static final BlockPos WEST = new BlockPos(0, 70, 0);

  @ParameterizedTest(name = "{0} blocks sag {1}")
  @CsvSource({"8,0.2", "32,0.8", "64,1.0"})
  void aLongerSpanSagsMoreUpToALimit(int length, double sag) {
    SpanCurve curve = SpanCurve.between(WEST, WEST.east(length), 0, 1);
    assertEquals(sag, curve.sag(), 1e-9);
    assertEquals(curve.from().y - sag, curve.at(0.5).y, 1e-9);
  }

  @ParameterizedTest(name = "t {0}")
  @CsvSource({"0,0.5", "1,32.5"})
  void theEndsSitOnTheCrossArms(double t, double x) {
    Vec3d end = SpanCurve.between(WEST, WEST.east(32), 0, 1).at(t);
    assertEquals(x, end.x, 1e-9);
    assertEquals(70 + SpanCurve.ARM_HEIGHT, end.y, 1e-9);
  }

  @ParameterizedTest(name = "lane {0} of {1}")
  @CsvSource({"0,1,0.5", "0,2,0.59375", "1,2,0.40625", "1,3,0.5"})
  void coloursOnOnePoleSitSideBySideAcrossTheLine(int lane, int lanes, double z) {
    SpanCurve curve = SpanCurve.between(WEST, WEST.east(16), lane, lanes);
    assertEquals(z, curve.from().z, 1e-9);
    assertEquals(z, curve.to().z, 1e-9);
  }

  @ParameterizedTest(name = "rise {0}")
  @CsvSource({"0", "6"})
  void theBoundsHoldTheWholeCurve(int rise) {
    SpanCurve curve = SpanCurve.between(WEST, WEST.east(30).up(rise), 0, 1);
    for (int step = 0; step <= 10; step++) {
      double y = curve.at(step / 10.0).y;
      assertEquals(true, y >= curve.low().y - 1e-9 && y <= curve.high().y + 1e-9);
    }
  }
}
