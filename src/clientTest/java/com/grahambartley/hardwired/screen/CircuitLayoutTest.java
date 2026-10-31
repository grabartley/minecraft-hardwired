package com.grahambartley.hardwired.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.grahambartley.hardwired.circuit.Side;
import com.grahambartley.hardwired.screen.CircuitLayout.Cell;
import com.grahambartley.hardwired.screen.CircuitLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class CircuitLayoutTest {

  private static final CircuitLayout LAYOUT = new CircuitLayout(0, 0);
  private static final Design DESIGN =
      new Design(
          List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
          List.of(Part.of(PartType.AND, 2, 3), Part.of(PartType.NOT, 4, 3)),
          List.of());

  @Test
  void theScreenFitsTheDefaultWindowAtGuiScaleTwo() {
    assertTrue(CircuitLayout.WIDTH <= 427);
    assertTrue(CircuitLayout.HEIGHT <= 240);
  }

  @Test
  void theColumnAndGridStayInsideTheCase() {
    assertTrue(LAYOUT.columnX() + LAYOUT.columnWidth() <= CircuitLayout.WIDTH - 2);
    assertTrue(LAYOUT.gridBottom() + CircuitLayout.PAD_DEPTH < LAYOUT.statusY());
    assertTrue(LAYOUT.statusY() + 9 <= CircuitLayout.HEIGHT);
  }

  @Test
  void thePaletteStopsAboveTheSettingRow() {
    Rect last = LAYOUT.paletteRow(PartType.values().length - 1);
    assertTrue(last.y() + last.height() <= LAYOUT.settingY());
  }

  @Test
  void aCentredLayoutStaysCentred() {
    CircuitLayout layout = CircuitLayout.centredIn(640, 360);
    assertEquals((640 - CircuitLayout.WIDTH) / 2, layout.left());
    assertEquals((360 - CircuitLayout.HEIGHT) / 2, layout.top());
  }

  @Test
  void theGridHoldsTheLargestAllowedDesign() {
    assertTrue(Design.GRID_WIDTH * Design.GRID_HEIGHT >= 256);
  }

  @ParameterizedTest(name = "click ({0}, {1}) lands on cell ({2}, {3})")
  @CsvSource({"16, 26, 0, 0", "29.9, 39.9, 0, 0", "30, 40, 1, 1", "295.9, 207.9, 19, 12"})
  void clicksLandOnTheirCell(double x, double y, int cellX, int cellY) {
    assertEquals(Optional.of(new Cell(cellX, cellY)), LAYOUT.cellAt(x, y));
  }

  @ParameterizedTest(name = "click ({0}, {1}) is off the grid")
  @CsvSource({"15, 26", "16, 25", "296, 26", "16, 208"})
  void clicksOffTheGridLandNowhere(double x, double y) {
    assertEquals(Optional.empty(), LAYOUT.cellAt(x, y));
  }

  @ParameterizedTest(name = "row {0} is {1}")
  @CsvSource({"0, AND", "6, SR_LATCH", "9, COUNTER"})
  void paletteRowsPickTheirPart(int row, PartType type) {
    Rect rect = LAYOUT.paletteRow(row);
    assertEquals(Optional.of(type), LAYOUT.paletteAt(rect.x() + 1, rect.y() + 1));
  }

  @Test
  void aClickBesideThePaletteFindsNothing() {
    assertEquals(Optional.empty(), LAYOUT.paletteAt(LAYOUT.columnX() - 1, 20));
  }

  @ParameterizedTest(name = "{0} pad finds itself")
  @EnumSource(Side.class)
  void padsAreFoundWhereTheyAreDrawn(Side side) {
    Rect pad = LAYOUT.pad(side);
    assertEquals(Optional.of(side), LAYOUT.padAt(pad.x() + 1, pad.y() + 1));
  }

  @ParameterizedTest(name = "{0} pad stays out of the grid")
  @EnumSource(Side.class)
  void padsDoNotCoverTheGrid(Side side) {
    Rect pad = LAYOUT.pad(side);
    assertFalse(LAYOUT.cellAt(pad.x() + 1, pad.y() + 1).isPresent());
  }

  @Test
  void anInputSideIsASourceAndAnOutputSideASink() {
    Rect north = LAYOUT.pad(Side.NORTH);
    Rect east = LAYOUT.pad(Side.EAST);
    Rect south = LAYOUT.pad(Side.SOUTH);
    assertEquals(
        Optional.of(new CircuitPin(0, 0, true)), LAYOUT.pinAt(DESIGN, north.x(), north.y()));
    assertEquals(
        Optional.of(new CircuitPin(1, 0, false)), LAYOUT.pinAt(DESIGN, east.x(), east.y()));
    assertEquals(Optional.empty(), LAYOUT.pinAt(DESIGN, south.x(), south.y()));
  }

  @ParameterizedTest(name = "{0} px in, {1} px down hits pin {2}, output {3}")
  @CsvSource({
    "0, 2, 0, false",
    "3, 6, 0, false",
    "1, 7, 1, false",
    "1, 12, 1, false",
    "10, 7, 0, true",
    "13, 2, 0, true"
  })
  void edgesOfATwoInputPartArePins(int insideX, int insideY, int pin, boolean output) {
    Part part = DESIGN.parts().get(0);
    assertEquals(
        Optional.of(new CircuitPin(Design.partNode(0), pin, output)),
        LAYOUT.pinAt(DESIGN, LAYOUT.cellX(part.x()) + insideX, LAYOUT.cellY(part.y()) + insideY));
  }

  @Test
  void bothHalvesOfAOneInputPartAreItsPin() {
    Part part = DESIGN.parts().get(1);
    assertEquals(
        Optional.of(new CircuitPin(Design.partNode(1), 0, false)),
        LAYOUT.pinAt(DESIGN, LAYOUT.cellX(part.x()) + 1, LAYOUT.cellY(part.y()) + 12));
  }

  @Test
  void theMiddleOfAPartIsNoPin() {
    Part part = DESIGN.parts().get(0);
    assertEquals(
        Optional.empty(),
        LAYOUT.pinAt(DESIGN, LAYOUT.cellX(part.x()) + 7, LAYOUT.cellY(part.y()) + 7));
  }

  @Test
  void emptyCellsHaveNoPins() {
    assertEquals(Optional.empty(), LAYOUT.pinAt(DESIGN, LAYOUT.cellX(9), LAYOUT.cellY(9)));
  }

  @Test
  void linePointsMeetTheirPins() {
    Part and = DESIGN.parts().get(0);
    assertEquals(LAYOUT.cellX(2), LAYOUT.inputPoint(and, 0).x());
    assertTrue(LAYOUT.inputPoint(and, 0).y() < LAYOUT.inputPoint(and, 1).y());
    assertEquals(LAYOUT.cellX(3) - 1, LAYOUT.outputPoint(and).x());
    assertEquals(LAYOUT.gridY(), LAYOUT.sidePoint(Side.NORTH).y());
    assertEquals(LAYOUT.gridRight() - 1, LAYOUT.sidePoint(Side.EAST).x());
  }

  @Test
  void sourceAndSinkPointsFollowTheNode() {
    assertEquals(LAYOUT.sidePoint(Side.NORTH), LAYOUT.sourcePoint(DESIGN, 0));
    assertEquals(
        LAYOUT.outputPoint(DESIGN.parts().get(0)), LAYOUT.sourcePoint(DESIGN, Design.partNode(0)));
    assertEquals(LAYOUT.sidePoint(Side.EAST), LAYOUT.sinkPoint(DESIGN, 1, 0));
    assertEquals(
        LAYOUT.inputPoint(DESIGN.parts().get(1), 0),
        LAYOUT.sinkPoint(DESIGN, Design.partNode(1), 0));
    assertFalse(new ArrayList<>(DESIGN.parts()).isEmpty());
  }
}
