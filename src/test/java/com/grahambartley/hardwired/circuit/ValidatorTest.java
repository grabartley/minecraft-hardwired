package com.grahambartley.hardwired.circuit;

import static com.grahambartley.hardwired.circuit.Designs.EAST;
import static com.grahambartley.hardwired.circuit.Designs.NORTH;
import static com.grahambartley.hardwired.circuit.Designs.SOUTH;
import static com.grahambartley.hardwired.circuit.Designs.part;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ValidatorTest {

  private static final int LIMIT = 64;
  private static final List<PortMode> SIDES = Designs.TWO_IN_ONE_OUT;

  private static Design parts(Part... parts) {
    return new Design(SIDES, List.of(parts), List.of());
  }

  private static Design lines(Line... lines) {
    return new Design(SIDES, List.of(Part.of(PartType.AND, 0, 0)), List.of(lines));
  }

  private static Stream<Arguments> refused() {
    return Stream.of(
        Arguments.of(
            "x below the grid", parts(new Part(PartType.AND, -1, 0, 0)), Refusal.OFF_THE_GRID),
        Arguments.of(
            "y below the grid", parts(new Part(PartType.AND, 0, -1, 0)), Refusal.OFF_THE_GRID),
        Arguments.of(
            "x past the grid",
            parts(new Part(PartType.AND, Design.GRID_WIDTH, 0, 0)),
            Refusal.OFF_THE_GRID),
        Arguments.of(
            "y past the grid",
            parts(new Part(PartType.AND, 0, Design.GRID_HEIGHT, 0)),
            Refusal.OFF_THE_GRID),
        Arguments.of(
            "huge x", parts(new Part(PartType.AND, Integer.MAX_VALUE, 0, 0)), Refusal.OFF_THE_GRID),
        Arguments.of(
            "two parts on one cell",
            parts(new Part(PartType.AND, 1, 1, 0), new Part(PartType.OR, 1, 1, 0)),
            Refusal.PARTS_OVERLAP),
        Arguments.of(
            "a gate with a setting", parts(new Part(PartType.AND, 1, 1, 1)), Refusal.BAD_SETTING),
        Arguments.of(
            "a timer of nought", parts(new Part(PartType.TIMER, 1, 1, 0)), Refusal.BAD_SETTING),
        Arguments.of(
            "a timer too long", parts(new Part(PartType.TIMER, 1, 1, 1201)), Refusal.BAD_SETTING),
        Arguments.of(
            "a counter of one", parts(new Part(PartType.COUNTER, 1, 1, 1)), Refusal.BAD_SETTING),
        Arguments.of(
            "a negative setting", parts(new Part(PartType.COUNTER, 1, 1, -4)), Refusal.BAD_SETTING),
        Arguments.of("a source past the nodes", lines(new Line(99, part(0), 0)), Refusal.BAD_LINE),
        Arguments.of("a negative source", lines(new Line(-1, part(0), 0)), Refusal.BAD_LINE),
        Arguments.of("a sink past the nodes", lines(new Line(NORTH, 99, 0)), Refusal.BAD_LINE),
        Arguments.of("a negative sink", lines(new Line(NORTH, -1, 0)), Refusal.BAD_LINE),
        Arguments.of("a negative pin", lines(new Line(NORTH, part(0), -1)), Refusal.BAD_LINE),
        Arguments.of("a pin past the part", lines(new Line(NORTH, part(0), 2)), Refusal.BAD_LINE),
        Arguments.of("a pin past a side", lines(new Line(part(0), EAST, 1)), Refusal.BAD_LINE),
        Arguments.of("from an output side", lines(new Line(EAST, part(0), 0)), Refusal.BAD_LINE),
        Arguments.of("from an unused side", lines(new Line(SOUTH, part(0), 0)), Refusal.BAD_LINE),
        Arguments.of("into an input side", lines(new Line(part(0), NORTH, 0)), Refusal.BAD_LINE),
        Arguments.of("into an unused side", lines(new Line(part(0), SOUTH, 0)), Refusal.BAD_LINE),
        Arguments.of(
            "two lines into one pin",
            lines(new Line(NORTH, part(0), 0), new Line(Designs.WEST, part(0), 0)),
            Refusal.INPUT_TAKEN),
        Arguments.of(
            "a loop with no delay",
            lines(new Line(part(0), part(0), 0)),
            Refusal.LOOP_WITHOUT_DELAY));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("refused")
  void badDesignsAreRefused(String name, Design design, Refusal reason) {
    assertEquals(Optional.of(reason), Validator.check(design, LIMIT));
  }

  @ParameterizedTest(name = "{0} parts against a limit of {1}: refused {2}")
  @CsvSource({"8, 8, false", "9, 8, true", "256, 256, false", "257, 256, true"})
  void thePartLimitComesFromTheCaller(int count, int limit, boolean refused) {
    List<Part> parts = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      parts.add(Part.of(PartType.AND, index % Design.GRID_WIDTH, index / Design.GRID_WIDTH));
    }
    Optional<Refusal> expected = refused ? Optional.of(Refusal.TOO_MANY_PARTS) : Optional.empty();
    assertEquals(expected, Validator.check(new Design(SIDES, parts, List.of()), limit));
  }

  @ParameterizedTest(name = "a working {0}")
  @EnumSource(PartType.class)
  void everyPartTypeCanBeValid(PartType type) {
    Design design =
        type.inputs() == 2
            ? Designs.twoInputPart(type, type.defaultSetting())
            : Designs.oneInputPart(type, type.defaultSetting());
    assertEquals(Optional.empty(), Validator.check(design, LIMIT));
  }

  @ParameterizedTest(name = "setting {1} on a {0} is accepted")
  @CsvSource({"TIMER, 1", "TIMER, 1200", "COUNTER, 2", "COUNTER, 256"})
  void settingsAtTheirLimitsAreAccepted(PartType type, int setting) {
    assertEquals(Optional.empty(), Validator.check(parts(new Part(type, 1, 1, setting)), LIMIT));
  }

  @ParameterizedTest(name = "a corner cell ({0}, {1})")
  @CsvSource({"0, 0", "19, 0", "0, 12", "19, 12"})
  void cornersOfTheGridAreOnIt(int x, int y) {
    assertEquals(Optional.empty(), Validator.check(parts(Part.of(PartType.OR, x, y)), LIMIT));
  }

  @Test
  void oneOutputMayFeedManyInputs() {
    Design design = lines(new Line(NORTH, part(0), 0), new Line(NORTH, part(0), 1));
    assertEquals(Optional.empty(), Validator.check(design, LIMIT));
  }

  @Test
  void aFloodOfLinesIsRefusedBeforeAnyWalk() {
    List<Line> flood = new ArrayList<>();
    for (int index = 0; index < 4096; index++) {
      flood.add(new Line(NORTH, part(0), 0));
    }
    Design design = new Design(SIDES, List.of(Part.of(PartType.AND, 0, 0)), flood);
    assertEquals(Optional.of(Refusal.INPUT_TAKEN), Validator.check(design, LIMIT));
  }

  @Test
  void aDesignWithoutFourSidesIsRefused() {
    Design design = new Design(List.of(PortMode.INPUT));
    assertEquals(Optional.of(Refusal.BAD_TEXT), Validator.check(design, LIMIT));
  }
}
