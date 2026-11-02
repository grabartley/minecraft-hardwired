package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LiftDialTest {

  private static final double CLOSE = 1.0E-9;
  private static final List<Integer> UNEVEN = List.of(0, 3, 10);

  @ParameterizedTest(name = "level {0} reads {1}")
  @CsvSource({"-5,0", "0,0", "1.5,0.25", "3,0.5", "6.5,0.75", "10,1", "40,1"})
  void aReadingFollowsTheFloorsNotTheHeight(double level, double reading) {
    assertEquals(reading, LiftDial.at(UNEVEN, 0).reading(level), CLOSE);
  }

  @ParameterizedTest(name = "{0} floors")
  @CsvSource({"0", "1"})
  void withNoRangeTheNeedleRestsAtTheStart(int floors) {
    List<Integer> levels = IntStream.range(0, floors).boxed().toList();
    assertEquals(0, LiftDial.at(levels, 0).reading(5), CLOSE);
  }

  @Test
  void aRestingDialHoldsItsLevelWhateverTheTime() {
    assertEquals(3, LiftDial.at(UNEVEN, 3).levelAt(9000), CLOSE);
  }

  @Test
  void aRidingDialStartsAtTheTripAndEndsAtIt() {
    LiftDial dial = LiftDial.riding(UNEVEN, new LiftTrip(0, 3, 20), 1000);
    assertEquals(0, dial.levelAt(1000), CLOSE);
    assertEquals(3, dial.levelAt(1060), CLOSE);
    assertEquals(3, dial.levelAt(5000), CLOSE);
  }

  @Test
  void aRidingDialMovesBetweenTicks() {
    LiftDial dial = LiftDial.riding(UNEVEN, new LiftTrip(0, 3, 20), 0);
    assertTrue(dial.levelAt(30.5) > dial.levelAt(30));
    assertTrue(dial.levelAt(31) > dial.levelAt(30.5));
  }

  @Test
  void theNeedleSweepsFromLeftToRight() {
    assertTrue(LiftDial.degrees(0) > 0);
    assertEquals(0, LiftDial.degrees(0.5), CLOSE);
    assertEquals(-LiftDial.degrees(0), LiftDial.degrees(1), CLOSE);
  }

  @ParameterizedTest(name = "{0} floors show {1} marks")
  @CsvSource({"1,0", "2,2", "9,9", "10,0"})
  void onlyAFloorCountThatFitsGetsMarks(int floors, int marks) {
    List<Integer> levels = IntStream.range(0, floors).boxed().toList();
    assertEquals(marks, LiftDial.at(levels, 0).markDegrees().size());
  }

  @ParameterizedTest(name = "level {0} lights mark {1}")
  @CsvSource({"0,0", "1,0", "2,1", "3,1", "6,1", "7,2", "10,2"})
  void theNearestFloorLightsUp(double level, int mark) {
    LiftDial dial = LiftDial.at(UNEVEN, 0);
    assertEquals(dial.markDegrees().get(mark), dial.litDegrees(level).orElseThrow(), 1.0E-4);
  }

  @Test
  void aDialWithNoMarksLightsNothing() {
    assertTrue(LiftDial.at(List.of(4), 4).litDegrees(4).isEmpty());
  }

  @Test
  void clearingRemovesEveryDialKey() {
    NbtCompound nbt = new NbtCompound();
    LiftDial.at(UNEVEN, 3).write(nbt);
    LiftDial.clear(nbt);
    assertTrue(nbt.isEmpty());
  }

  @Test
  void aDialSurvivesASave() {
    LiftDial dial = LiftDial.riding(UNEVEN, new LiftTrip(1.5, 10, 20), 77);
    NbtCompound nbt = new NbtCompound();
    dial.write(nbt);
    assertEquals(dial, LiftDial.read(nbt));
  }
}
