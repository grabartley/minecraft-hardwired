package com.grahambartley.hardwired.circuit;

import static com.grahambartley.hardwired.circuit.Designs.EAST;
import static com.grahambartley.hardwired.circuit.Designs.NORTH;
import static com.grahambartley.hardwired.circuit.Designs.part;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class EvaluatorTest {

  private static final int FIRST = Side.NORTH.bit();
  private static final int SECOND = Side.WEST.bit();
  private static final int BOTH = FIRST | SECOND;
  private static final int NONE = 0;
  private static final int OUT = Side.EAST.bit();

  private static boolean run(Evaluator evaluator, int... inputs) {
    for (int input : inputs) {
      evaluator.step(input);
    }
    return evaluator.outputs() == OUT;
  }

  @ParameterizedTest(name = "one step apart at world time {0} comes {1} ticks later")
  @CsvSource({"0, 2", "1, 1", "2, 2", "3, 1", "-1, 1"})
  void stepsLandOnEvenTicks(long time, int delay) {
    assertEquals(delay, Evaluator.delayToNextStep(time));
  }

  @Test
  void oneEvaluationEveryTwoGameTicks() {
    assertEquals(2, Evaluator.TICKS_PER_STEP);
  }

  @ParameterizedTest(name = "{0} with inputs {1}: {2}")
  @CsvSource({
    "AND, 0, false",
    "AND, 1, false",
    "AND, 8, false",
    "AND, 9, true",
    "OR, 0, false",
    "OR, 1, true",
    "OR, 8, true",
    "XOR, 9, false",
    "XOR, 8, true",
    "NAND, 9, false",
    "NAND, 0, true",
    "NOR, 0, true",
    "NOR, 8, false"
  })
  void gatesAnswerInTheStepThatFeedsThem(PartType type, int inputs, boolean on) {
    assertEquals(on, run(new Evaluator(Designs.twoInputPart(type, 0)), inputs));
  }

  @Test
  void anUnwiredInputReadsOff() {
    Design design =
        new Design(
            Designs.TWO_IN_ONE_OUT,
            List.of(new Part(PartType.NOT, 1, 1, 0)),
            List.of(new Line(part(0), EAST, 0)));
    assertTrue(run(new Evaluator(design), BOTH));
  }

  @Test
  void aSideThatIsNotAnInputReadsOff() {
    Design design =
        new Design(
            List.of(PortMode.UNUSED, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
            List.of(new Part(PartType.NOT, 1, 1, 0)),
            List.of(new Line(part(0), EAST, 0)));
    assertTrue(run(new Evaluator(design), FIRST | Side.SOUTH.bit()));
  }

  @Test
  void gatesWorkInFeedOrderWhateverOrderTheyWerePlaced() {
    Design chain =
        new Design(
            Designs.TWO_IN_ONE_OUT,
            List.of(new Part(PartType.NOT, 1, 1, 0), new Part(PartType.NOT, 2, 1, 0)),
            List.of(
                new Line(NORTH, part(1), 0),
                new Line(part(1), part(0), 0),
                new Line(part(0), EAST, 0)));
    assertTrue(run(new Evaluator(chain), FIRST));
    assertFalse(run(new Evaluator(chain), NONE));
  }

  private static Stream<Arguments> latchRuns() {
    return Stream.of(
        Arguments.of("set shows a step later", PartType.SR_LATCH, 0, new int[] {FIRST}, false),
        Arguments.of("set holds", PartType.SR_LATCH, 0, new int[] {FIRST, NONE, NONE}, true),
        Arguments.of("reset clears", PartType.SR_LATCH, 0, new int[] {FIRST, SECOND, NONE}, false),
        Arguments.of("reset wins", PartType.SR_LATCH, 0, new int[] {BOTH, BOTH}, false),
        Arguments.of("data stored", PartType.D_LATCH, 0, new int[] {BOTH, NONE}, true),
        Arguments.of("data ignored", PartType.D_LATCH, 0, new int[] {FIRST, FIRST}, false),
        Arguments.of("data cleared", PartType.D_LATCH, 0, new int[] {BOTH, SECOND, NONE}, false),
        Arguments.of("counts to two", PartType.COUNTER, 2, new int[] {FIRST, NONE}, true),
        Arguments.of(
            "two counts wrap", PartType.COUNTER, 2, new int[] {FIRST, NONE, FIRST, NONE}, false),
        Arguments.of(
            "held clock counts once", PartType.COUNTER, 3, new int[] {FIRST, FIRST}, false),
        Arguments.of(
            "reaches the last count",
            PartType.COUNTER,
            3,
            new int[] {FIRST, NONE, FIRST, NONE},
            true),
        Arguments.of(
            "reset clears the count", PartType.COUNTER, 2, new int[] {FIRST, SECOND, NONE}, false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("latchRuns")
  void delayPartsHoldState(String name, PartType type, int setting, int[] inputs, boolean on) {
    assertEquals(on, run(new Evaluator(Designs.twoInputPart(type, setting)), inputs));
  }

  @ParameterizedTest(name = "a timer of {0} steps turns on after it")
  @CsvSource({"1", "3", "20"})
  void aTimerWaitsThenTurnsOn(int delay) {
    Evaluator evaluator = new Evaluator(Designs.oneInputPart(PartType.TIMER, delay));
    for (int step = 0; step < delay; step++) {
      assertFalse(run(evaluator, FIRST), "step " + step);
    }
    assertTrue(run(evaluator, FIRST));
    run(evaluator, NONE);
    assertFalse(run(evaluator, NONE));
  }

  @Test
  void aTimerThatDropsEarlyStartsAgain() {
    Evaluator evaluator = new Evaluator(Designs.oneInputPart(PartType.TIMER, 3));
    run(evaluator, FIRST, FIRST, NONE, FIRST, FIRST);
    assertFalse(run(evaluator, FIRST));
    assertTrue(run(evaluator, FIRST));
  }

  @ParameterizedTest(name = "a loop with a timer of {0} is a clock")
  @CsvSource({"1", "4"})
  void aTimerInALoopIsAClock(int delay) {
    Evaluator evaluator = new Evaluator(Designs.notThroughTimerOscillator(delay));
    int changes = 0;
    boolean last = false;
    for (int step = 0; step < 60; step++) {
      evaluator.step(NONE);
      boolean now = evaluator.outputs() == OUT;
      changes += now != last ? 1 : 0;
      last = now;
    }
    assertTrue(changes >= 10, "changes " + changes);
  }

  @Test
  void aDesignThatStoppedChangingIsSettled() {
    Evaluator evaluator = new Evaluator(Designs.oneInputPart(PartType.TIMER, 2));
    evaluator.step(FIRST);
    assertFalse(evaluator.isSettled());
    evaluator.step(FIRST);
    evaluator.step(FIRST);
    assertTrue(evaluator.isSettled());
  }

  @Test
  void aSteppedPlainGateIsSettled() {
    Evaluator evaluator = new Evaluator(Designs.twoInputPart(PartType.AND, 0));
    evaluator.step(BOTH);
    assertTrue(evaluator.isSettled());
  }

  @Test
  void aRunningClockNeverSettles() {
    Evaluator evaluator = new Evaluator(Designs.notThroughTimerOscillator(1));
    for (int step = 0; step < 20; step++) {
      evaluator.step(NONE);
      assertFalse(evaluator.isSettled());
    }
  }

  @Test
  void restoringCarriesOnFromTheSavedPlace() {
    Design design = Designs.oneInputPart(PartType.TIMER, 4);
    Evaluator saved = new Evaluator(design);
    run(saved, FIRST, FIRST);
    Evaluator loaded = new Evaluator(design);
    loaded.restore(saved.memory(), saved.outputs());
    assertArrayEquals(saved.memory(), loaded.memory());
    assertFalse(run(loaded, FIRST, FIRST));
    assertTrue(run(loaded, FIRST));
  }

  private static Stream<Arguments> badMemory() {
    return Stream.of(
        Arguments.of(PartType.SR_LATCH, 0, -7, 1),
        Arguments.of(PartType.D_LATCH, 0, 99, 1),
        Arguments.of(PartType.TIMER, 5, 900, 5),
        Arguments.of(PartType.TIMER, 5, -1, 0),
        Arguments.of(PartType.COUNTER, 4, Integer.MAX_VALUE, 7),
        Arguments.of(PartType.COUNTER, 4, -2, 0));
  }

  @ParameterizedTest(name = "{0} loaded with {2} keeps {3}")
  @MethodSource("badMemory")
  void hostileSavedMemoryIsClamped(PartType type, int setting, int stored, int kept) {
    Evaluator evaluator = new Evaluator(Designs.twoInputPart(type, setting));
    evaluator.restore(new int[] {stored}, 0);
    assertEquals(kept, evaluator.memory()[0]);
  }

  @Test
  void savedMemoryOfTheWrongLengthIsIgnored() {
    Evaluator evaluator = new Evaluator(Designs.twoInputPart(PartType.SR_LATCH, 0));
    evaluator.restore(new int[] {1, 1, 1}, OUT);
    assertArrayEquals(new int[] {0}, evaluator.memory());
    assertEquals(0, evaluator.outputs());
  }

  @Test
  void savedOutputsOnlyCoverOutputSides() {
    Evaluator evaluator = new Evaluator(Designs.twoInputPart(PartType.SR_LATCH, 0));
    evaluator.restore(new int[] {1}, 0xFF);
    assertEquals(OUT, evaluator.outputs());
  }

  @Test
  void aLoopWithoutDelayCannotBeRun() {
    Design loop =
        new Design(
            Designs.TWO_IN_ONE_OUT,
            List.of(new Part(PartType.NOT, 1, 1, 0)),
            List.of(new Line(part(0), part(0), 0)));
    assertThrows(IllegalArgumentException.class, () -> new Evaluator(loop));
  }

  @Test
  void levelsShowEachLine() {
    Evaluator evaluator = new Evaluator(Designs.twoInputPart(PartType.AND, 0));
    evaluator.step(BOTH);
    assertTrue(evaluator.isOn(part(0)));
    assertTrue(evaluator.isOn(NORTH));
    assertFalse(evaluator.isOn(EAST));
  }
}
