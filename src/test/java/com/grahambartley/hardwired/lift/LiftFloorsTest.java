package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class LiftFloorsTest {

  private static final List<Integer> FLOORS = List.of(10, 14, 20);

  static Stream<Arguments> unsorted() {
    return Stream.of(
        Arguments.of(List.of(20, 10, 14), FLOORS),
        Arguments.of(List.of(14, 14, 10, 20, 10), FLOORS),
        Arguments.of(List.of(-5, 3), List.of(-5, 3)),
        Arguments.of(List.of(), List.of()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unsorted")
  void floorsAreOrderedByHeightWithOneEntryPerLevel(List<Integer> levels, List<Integer> ordered) {
    assertEquals(ordered, LiftFloors.ordered(levels));
  }

  @Test
  void theFloorListIsCapped() {
    List<Integer> many = IntStream.range(0, 1000).boxed().toList();
    assertEquals(LiftFloors.MAX_FLOORS, LiftFloors.ordered(many).size());
  }

  @ParameterizedTest(name = "level {0}")
  @CsvSource({"10,1", "14,2", "20,3", "15,0"})
  void floorsAreNumberedFromTheBottom(int level, int number) {
    assertEquals(number, LiftFloors.number(FLOORS, level));
  }

  @ParameterizedTest(name = "from {0}")
  @CsvSource({"20,20", "19.5,14", "14,14", "10,10"})
  void theFloorBelowIsTheNearestOneDown(double level, int floor) {
    assertEquals(OptionalInt.of(floor), LiftFloors.atOrBelow(FLOORS, level));
  }

  @ParameterizedTest(name = "from {0} direction {1}")
  @CsvSource({"12,1,10", "12,-1,14", "14,1,14", "14,-1,14"})
  void theLastSafeFloorIsBehindTheCar(double level, int direction, int floor) {
    assertEquals(OptionalInt.of(floor), LiftFloors.lastSafe(FLOORS, level, direction));
  }

  @Test
  void belowTheLowestFloorThereIsNothing() {
    assertTrue(LiftFloors.atOrBelow(FLOORS, 9.5).isEmpty());
    assertTrue(LiftFloors.atOrAbove(FLOORS, 20.5).isEmpty());
  }
}
