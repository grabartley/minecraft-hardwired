package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DeskLayoutTest {

  private static final BlockPos PANEL = new BlockPos(10, 64, -3);

  @ParameterizedTest(name = "{0} places {1}")
  @CsvSource({"NORTH,WEST", "EAST,NORTH", "SOUTH,EAST", "WEST,SOUTH"})
  void thePanelGoesOnThePlayersLeft(Direction playerFacing, Direction expected) {
    assertEquals(expected, DeskLayout.facingFor(playerFacing));
  }

  @ParameterizedTest(name = "{0} {1}")
  @CsvSource({
    "PANEL,NORTH", "PANEL,EAST", "PANEL,SOUTH", "PANEL,WEST",
    "TOP,NORTH", "TOP,EAST", "TOP,SOUTH", "TOP,WEST",
    "DESK,NORTH", "DESK,EAST", "DESK,SOUTH", "DESK,WEST"
  })
  void everyCellFindsThePanel(DeskPart part, Direction facing) {
    BlockPos cell = DeskLayout.cell(part, facing, PANEL);
    assertEquals(PANEL, DeskLayout.panelOf(part, facing, cell));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "EAST", "SOUTH", "WEST"})
  void theDeskIsThreeCellsWithTheDeskBehindThePanelLikeABed(Direction facing) {
    assertEquals(3, new HashSet<>(DeskLayout.cells(facing, PANEL)).size());
    assertEquals(PANEL, DeskLayout.cell(DeskPart.DESK, facing, PANEL).offset(facing));
    assertEquals(PANEL.up(), DeskLayout.cell(DeskPart.TOP, facing, PANEL));
  }
}
