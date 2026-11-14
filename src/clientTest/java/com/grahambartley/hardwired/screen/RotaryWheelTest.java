package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RotaryWheelTest {

  private static final int RING = 40;
  private static final int HOLE = 6;

  @ParameterizedTest(name = "digit {0}")
  @CsvSource({"1,1", "5,5", "9,9", "0,10"})
  void zeroSendsTenPulsesAndEveryOtherDigitItsOwn(int digit, int pulses) {
    assertEquals(pulses, RotaryWheel.pulsesFor(digit));
    assertEquals(digit, RotaryWheel.digitFor(pulses));
  }

  @ParameterizedTest(name = "pulses {0}")
  @ValueSource(ints = {1, 2, 5, 9, 10})
  void everyHoleTurnsExactlyToTheFingerStop(int pulses) {
    double reached = RotaryWheel.holeAngle(pulses) + RotaryWheel.travel(pulses);
    assertEquals(RotaryWheel.STOP, reached, 1e-9);
  }

  @ParameterizedTest(name = "pulses {0}")
  @ValueSource(ints = {1, 3, 7, 10})
  void aPressOnAHoleFindsThatHole(int pulses) {
    double angle = RotaryWheel.holeAngle(pulses);
    assertEquals(
        pulses, RotaryWheel.holeAt(Math.cos(angle) * RING, Math.sin(angle) * RING, RING, HOLE));
  }

  @ParameterizedTest(name = "at {0},{1}")
  @CsvSource({"0,0", "20,34.6", "60,0"})
  void aPressOffTheHolesFindsNothing(double dx, double dy) {
    assertEquals(0, RotaryWheel.holeAt(dx, dy, RING, HOLE));
  }

  @ParameterizedTest(name = "dragged {1} degrees with {0} pulses")
  @CsvSource({"1,30,30", "1,60,60", "1,90,60", "1,200,60", "1,300,0", "5,-10,0", "10,330,330"})
  void theWheelTurnsClockwiseOnlyAndStopsAtTheFingerStop(
      int pulses, double degrees, double expected) {
    double from = RotaryWheel.holeAngle(pulses);
    double turned = RotaryWheel.turn(from, from + Math.toRadians(degrees), pulses);
    assertEquals(expected, Math.toDegrees(turned), 1e-6);
  }

  @ParameterizedTest(name = "pulses {0} turned {1}")
  @CsvSource({"1,60,true", "1,59.5,true", "1,58,false", "10,330,true", "10,300,false"})
  void aDigitCountsOnlyAtTheFingerStop(int pulses, double degrees, boolean dialed) {
    assertEquals(dialed, RotaryWheel.atStop(Math.toRadians(degrees), pulses));
  }
}
