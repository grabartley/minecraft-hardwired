package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ExchangeLayoutTest {

  private static final BlockPos ANCHOR = new BlockPos(10, 64, -3);

  @ParameterizedTest(name = "facing {0} extends {1}")
  @CsvSource({"NORTH,WEST", "SOUTH,EAST", "EAST,NORTH", "WEST,SOUTH"})
  void theBoardExtendsToTheRightOfAPlayerFacingIt(Direction facing, Direction right) {
    assertEquals(right, ExchangeLayout.rightOf(facing));
    assertEquals(facing.getOpposite().rotateYClockwise(), ExchangeLayout.rightOf(facing));
  }

  @ParameterizedTest(name = "facing {0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void everyPartFindsTheSameAnchor(Direction facing) {
    for (ExchangePart part : ExchangePart.values()) {
      BlockPos cell = ExchangeLayout.cell(part, facing, ANCHOR);
      assertEquals(ANCHOR, ExchangeLayout.anchorOf(part, facing, cell));
    }
  }

  @ParameterizedTest(name = "facing {0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void theBoardIsTwoWideAndTwoTall(Direction facing) {
    List<BlockPos> cells = ExchangeLayout.cells(facing, ANCHOR);
    Set<BlockPos> distinct = new HashSet<>(cells);
    assertEquals(4, distinct.size());
    assertEquals(ANCHOR, cells.getFirst());
    assertEquals(2, distinct.stream().filter(cell -> cell.getY() == ANCHOR.getY() + 1).count());
    assertEquals(
        2,
        distinct.stream()
            .filter(
                cell ->
                    cell.equals(ANCHOR)
                        || cell.equals(ANCHOR.offset(ExchangeLayout.rightOf(facing))))
            .count());
  }
}
