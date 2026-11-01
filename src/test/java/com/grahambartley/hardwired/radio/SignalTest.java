package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.config.RadioSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SignalTest {

  private static final double EPSILON = 1e-9;

  @ParameterizedTest(name = "{0} of {1} is {2}")
  @CsvSource({"0,100,1", "50,100,0.5", "100,100,0", "150,100,0", "5,0,0"})
  void signalFallsEvenlyToTheEdgeOfReach(double distance, int reach, double expected) {
    assertEquals(expected, Signal.byDistance(distance, reach), EPSILON);
  }

  @ParameterizedTest(name = "depth {0} gives {1}")
  @CsvSource({"-3,1", "0,1", "8,1", "28,0.5", "48,0", "90,0"})
  void depthCostsNothingUntilTheFreeDepthThenFallsToNothing(int depth, double expected) {
    assertEquals(expected, Signal.byDepth(depth, 8, 40), EPSILON);
  }

  @ParameterizedTest(name = "{0} steps off gives {1}")
  @CsvSource({"0,1", "1,0.5", "2,0", "50,0"})
  void oneStepOffIsHalfAndFurtherIsNothing(int steps, double expected) {
    assertEquals(expected, Signal.byTuning(steps), EPSILON);
  }

  @ParameterizedTest(name = "at {0}, depth {2}, off {3}")
  @CsvSource({
    "0,100,1,0,1",
    "50,100,1,0,0.5",
    "50,100,1,1,0.25",
    "50,100,28,0,0.25",
    "0,100,48,0,0",
    "0,100,1,2,0"
  })
  void strengthMultipliesAllThreeCuts(
      double distance, int reach, int depth, int steps, double expected) {
    assertEquals(
        expected, Signal.strength(distance, reach, depth, steps, RadioSettings.DEFAULTS), EPSILON);
  }

  @ParameterizedTest(name = "{0} shows {1}")
  @CsvSource({"0,0", "0.01,1", "0.2,1", "0.21,2", "0.5,3", "0.81,5", "1,5", "2,5"})
  void theNeedleHasFiveSteps(double strength, int needle) {
    assertEquals(needle, Signal.needle(strength));
  }
}
