package com.grahambartley.hardwired.circuit;

import static com.grahambartley.hardwired.circuit.Designs.EAST;
import static com.grahambartley.hardwired.circuit.Designs.NORTH;
import static com.grahambartley.hardwired.circuit.Designs.part;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class LoopCheckTest {

  private static Design parts(List<Line> lines, PartType... types) {
    List<Part> parts = new ArrayList<>();
    for (int index = 0; index < types.length; index++) {
      parts.add(Part.of(types[index], index, 0));
    }
    return new Design(Designs.TWO_IN_ONE_OUT, parts, lines);
  }

  private static Stream<Arguments> designs() {
    return Stream.of(
        Arguments.of("empty", Design.EMPTY, false),
        Arguments.of("a straight run", Designs.twoInputPart(PartType.AND, 0), false),
        Arguments.of(
            "a gate into itself",
            parts(List.of(new Line(part(0), part(0), 0)), PartType.NOT),
            true),
        Arguments.of(
            "two gates round",
            parts(
                List.of(new Line(part(0), part(1), 0), new Line(part(1), part(0), 0)),
                PartType.NOR,
                PartType.NOR),
            true),
        Arguments.of(
            "a loop off to the side of a good line",
            parts(
                List.of(
                    new Line(NORTH, part(0), 0),
                    new Line(part(0), part(1), 0),
                    new Line(part(1), part(2), 0),
                    new Line(part(2), part(1), 1),
                    new Line(part(0), EAST, 0)),
                PartType.NOT,
                PartType.OR,
                PartType.NOT),
            true),
        Arguments.of(
            "a diamond rejoining",
            parts(diamond(), PartType.NOT, PartType.NOT, PartType.AND),
            false));
  }

  private static List<Line> diamond() {
    return List.of(
        new Line(NORTH, part(0), 0),
        new Line(NORTH, part(1), 0),
        new Line(part(0), part(2), 0),
        new Line(part(1), part(2), 1));
  }

  @ParameterizedTest(name = "{0}: loop {2}")
  @MethodSource("designs")
  void loopsWithoutDelayAreFound(String name, Design design, boolean loop) {
    assertEquals(loop, LoopCheck.hasLoop(design));
  }

  @ParameterizedTest(name = "a {0} breaks a loop")
  @EnumSource(
      value = PartType.class,
      names = {"SR_LATCH", "D_LATCH", "TIMER", "COUNTER"})
  void delayPartsBreakLoops(PartType delay) {
    Design loop =
        parts(
            List.of(new Line(part(0), part(1), 0), new Line(part(1), part(0), 0)),
            PartType.NOT,
            delay);
    assertFalse(LoopCheck.hasLoop(loop));
  }

  @Test
  void gatesComeOutAfterTheGatesThatFeedThem() {
    Design chain =
        parts(
            List.of(new Line(part(2), part(1), 0), new Line(part(1), part(0), 0)),
            PartType.NOT,
            PartType.NOT,
            PartType.NOT);
    assertEquals(
        List.of(part(2), part(1), part(0)),
        LoopCheck.gateOrder(chain).stream()
            .flatMapToInt(java.util.Arrays::stream)
            .boxed()
            .toList());
  }

  @Test
  void aLongChainNeedsNoDeepStack() {
    List<Part> parts = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
    for (int index = 0; index < 5000; index++) {
      parts.add(new Part(PartType.NOT, 0, 0, 0));
      if (index > 0) {
        lines.add(new Line(part(index - 1), part(index), 0));
      }
    }
    assertTrue(LoopCheck.gateOrder(new Design(Designs.TWO_IN_ONE_OUT, parts, lines)).isPresent());
  }
}
