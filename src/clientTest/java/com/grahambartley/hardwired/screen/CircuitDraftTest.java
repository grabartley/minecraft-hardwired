package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CircuitDraftTest {

  private static final int FIRST = Design.partNode(0);
  private static final int SECOND = Design.partNode(1);
  private static final int THIRD = Design.partNode(2);

  private static CircuitDraft three() {
    CircuitDraft draft = new CircuitDraft(Design.EMPTY);
    draft.place(PartType.NOT, 0, 0, 64);
    draft.place(PartType.AND, 1, 0, 64);
    draft.place(PartType.OR, 2, 0, 64);
    return draft;
  }

  private static CircuitPin out(int node) {
    return new CircuitPin(node, 0, true);
  }

  private static CircuitPin in(int node, int pin) {
    return new CircuitPin(node, pin, false);
  }

  @ParameterizedTest(name = "placing at ({0}, {1}): {2}")
  @CsvSource({
    "0, 0, true",
    "19, 12, true",
    "20, 0, false",
    "0, 13, false",
    "-1, 0, false",
    "1, 0, false"
  })
  void partsLandOnFreeCellsOfTheGrid(int x, int y, boolean placed) {
    CircuitDraft draft = new CircuitDraft(Design.EMPTY);
    draft.place(PartType.OR, 1, 0, 64);
    assertEquals(placed, draft.place(PartType.AND, x, y, 64));
  }

  @Test
  void theLimitStopsPlacing() {
    CircuitDraft draft = three();
    assertFalse(draft.place(PartType.NOT, 5, 5, 3));
    assertEquals(OptionalInt.empty(), draft.nodeAt(5, 5));
  }

  @Test
  void aPartMovesOnlyToAFreeCell() {
    CircuitDraft draft = three();
    assertFalse(draft.move(FIRST, 1, 0));
    assertTrue(draft.move(FIRST, 7, 7));
    assertEquals(OptionalInt.of(FIRST), draft.nodeAt(7, 7));
  }

  @Test
  void removingAPartDropsItsLinesAndRenumbersTheRest() {
    CircuitDraft draft = three();
    draft.connect(out(FIRST), in(SECOND, 0));
    draft.connect(out(SECOND), in(THIRD, 1));
    draft.connect(out(FIRST), in(THIRD, 0));
    draft.remove(FIRST);
    Design design = draft.design();
    assertEquals(PartType.AND, design.partAt(FIRST).type());
    assertEquals(List.of(new Line(FIRST, SECOND, 1)), design.lines());
  }

  @Test
  void aNewLineReplacesTheOldOneOnThatPin() {
    CircuitDraft draft = three();
    draft.connect(out(FIRST), in(THIRD, 0));
    draft.connect(out(SECOND), in(THIRD, 0));
    assertEquals(List.of(new Line(SECOND, THIRD, 0)), draft.design().lines());
  }

  @Test
  void aLineCanBeDrawnFromEitherEnd() {
    CircuitDraft draft = three();
    assertTrue(draft.connect(in(SECOND, 1), out(FIRST)));
    assertEquals(List.of(new Line(FIRST, SECOND, 1)), draft.design().lines());
  }

  @Test
  void twoInputsOrTwoOutputsDoNotJoin() {
    CircuitDraft draft = three();
    assertFalse(draft.connect(in(FIRST, 0), in(SECOND, 0)));
    assertFalse(draft.connect(out(FIRST), out(SECOND)));
    assertFalse(draft.connect(out(FIRST), in(FIRST, 0)));
    assertTrue(draft.design().lines().isEmpty());
  }

  @Test
  void disconnectingAnOutputCutsAllItsLines() {
    CircuitDraft draft = three();
    draft.connect(out(FIRST), in(SECOND, 0));
    draft.connect(out(FIRST), in(THIRD, 0));
    draft.disconnect(out(FIRST));
    assertTrue(draft.design().lines().isEmpty());
  }

  @Test
  void disconnectingAnInputCutsOnlyThatPin() {
    CircuitDraft draft = three();
    draft.connect(out(FIRST), in(SECOND, 0));
    draft.connect(out(FIRST), in(SECOND, 1));
    draft.disconnect(in(SECOND, 0));
    assertEquals(List.of(new Line(FIRST, SECOND, 1)), draft.design().lines());
  }

  @ParameterizedTest(name = "{0} next is {1}")
  @CsvSource({"UNUSED, INPUT", "INPUT, OUTPUT", "OUTPUT, UNUSED"})
  void cyclingASideMovesItsMode(PortMode start, PortMode next) {
    CircuitDraft draft = new CircuitDraft(new Design(List.of(start, start, start, start)));
    draft.cycleSide(Side.EAST);
    assertEquals(next, draft.design().mode(Side.EAST));
    assertEquals(start, draft.design().mode(Side.WEST));
  }

  @Test
  void cyclingASideDropsItsLines() {
    CircuitDraft draft =
        new CircuitDraft(
            new Design(
                List.of(PortMode.INPUT, PortMode.UNUSED, PortMode.UNUSED, PortMode.UNUSED),
                List.of(Part.of(PartType.NOT, 0, 0)),
                List.of(new Line(0, FIRST, 0))));
    draft.cycleSide(Side.NORTH);
    assertTrue(draft.design().lines().isEmpty());
  }

  @ParameterizedTest(name = "{0} at {1} changed by {2} is {3}")
  @CsvSource({
    "TIMER, 10, 1, 11",
    "TIMER, 10, -10, 1",
    "TIMER, 1200, 10, 1200",
    "COUNTER, 2, -1, 2",
    "COUNTER, 256, 1, 256"
  })
  void settingsStayInRange(PartType type, int start, int change, int expected) {
    CircuitDraft draft =
        new CircuitDraft(
            new Design(Design.EMPTY.sides(), List.of(new Part(type, 0, 0, start)), List.of()));
    draft.adjustSetting(FIRST, change);
    assertEquals(expected, draft.design().partAt(FIRST).setting());
  }

  @Test
  void aGateHasNoSettingToAdjust() {
    CircuitDraft draft = three();
    draft.adjustSetting(FIRST, 5);
    assertEquals(0, draft.design().partAt(FIRST).setting());
  }

  @Test
  void loadingReplacesEverythingAndClearingEmpties() {
    Design design = three().design();
    CircuitDraft draft = new CircuitDraft(Design.EMPTY);
    draft.load(design);
    assertEquals(design, draft.design());
    draft.clear();
    assertEquals(Design.EMPTY, draft.design());
  }
}
