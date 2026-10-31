package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DesignTest {

  @ParameterizedTest(name = "{0} turns gives {1}")
  @CsvSource({"0, 'iouo'", "1, 'oiou'", "2, 'uoio'", "3, 'ouoi'", "4, 'iouo'"})
  void sidesTurnIntoWorldSides(int turns, String expected) {
    Design design =
        new Design(List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.OUTPUT));
    StringBuilder codes = new StringBuilder();
    design.sidesTurned(turns).forEach(mode -> codes.append(mode.code()));
    assertEquals(expected, codes.toString());
  }

  @ParameterizedTest(name = "node {0} is a side: {1}")
  @CsvSource({"0, true", "3, true", "4, false", "-1, false"})
  void theFirstFourNodesAreSides(int node, boolean side) {
    assertEquals(side, Design.EMPTY.isSide(node));
  }

  @Test
  void partsComeAfterTheSides() {
    Design design = Designs.oneInputPart(PartType.NOT, 0);
    assertEquals(PartType.NOT, design.partAt(Design.partNode(0)).type());
    assertEquals(Side.COUNT + 1, design.nodes());
  }

  @Test
  void aDesignWithNoPartsIsEmpty() {
    assertTrue(Design.EMPTY.isEmpty());
    assertFalse(Designs.oneInputPart(PartType.NOT, 0).isEmpty());
  }
}
