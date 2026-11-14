package com.grahambartley.hardwired.client.line;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.block.TelephonePoleBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanCurveTest {

  private static final BlockPos WEST = new BlockPos(0, 70, 0);

  private static SpanCurve.End end(BlockPos top, Direction.Axis arm, int lane, int lanes) {
    return new SpanCurve.End(top, arm, lane, lanes);
  }

  private static SpanCurve single(BlockPos to) {
    return SpanCurve.between(end(WEST, Direction.Axis.Z, 0, 1), end(to, Direction.Axis.Z, 0, 1));
  }

  @ParameterizedTest(name = "{0} blocks sag {1}")
  @CsvSource({"8,0.2", "32,0.8", "64,1.0"})
  void aLongerSpanSagsMoreUpToALimit(int length, double sag) {
    SpanCurve curve = single(WEST.east(length));
    assertEquals(sag, curve.sag(), 1e-9);
    assertEquals(curve.from().y - sag, curve.at(0.5).y, 1e-9);
  }

  @ParameterizedTest(name = "t {0}")
  @CsvSource({"0,0.5", "1,32.5"})
  void aLoneColourSitsOnTheMiddleOfTheCrossArm(double t, double x) {
    Vec3d point = single(WEST.east(32)).at(t);
    assertEquals(x, point.x, 1e-9);
    assertEquals(0.5, point.z, 1e-9);
    assertEquals(70 + TelephonePoleBlock.ARM_HEIGHT, point.y, 1e-9);
  }

  @ParameterizedTest(name = "lane {0} of {1} on an {2} arm")
  @CsvSource({
    "0,2,Z,0.5,0.28125",
    "1,2,Z,0.5,0.71875",
    "0,2,X,0.28125,0.5",
    "1,3,X,0.5,0.5",
    "0,8,Z,0.5,0.1171875"
  })
  void coloursSpreadAlongThatPolesOwnArm(
      int lane, int lanes, Direction.Axis arm, double x, double z) {
    Vec3d point =
        SpanCurve.between(end(WEST, arm, lane, lanes), end(WEST.east(16), arm, 0, 1)).from();
    assertEquals(x, point.x, 1e-9);
    assertEquals(z, point.z, 1e-9);
  }

  @ParameterizedTest(name = "rise {0}")
  @CsvSource({"0", "6"})
  void theBoundsHoldTheWholeCurve(int rise) {
    SpanCurve curve = single(WEST.east(30).up(rise));
    for (int step = 0; step <= 10; step++) {
      double y = curve.at(step / 10.0).y;
      assertTrue(y >= curve.low().y - 1e-9 && y <= curve.high().y + 1e-9);
    }
  }
}
