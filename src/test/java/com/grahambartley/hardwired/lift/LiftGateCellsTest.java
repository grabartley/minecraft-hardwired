package com.grahambartley.hardwired.lift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class LiftGateCellsTest {

  private static final BlockPos POS = new BlockPos(10, 64, -20);

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void aGateIsTwoWideAndTwoTall(Direction facing) {
    List<BlockPos> cells = LiftGateCells.cells(POS, facing);
    assertEquals(4, new HashSet<>(cells).size());
    assertEquals(POS.up(), cells.get(1));
    assertEquals(cells.get(2).up(), cells.get(3));
    assertEquals(1, POS.getManhattanDistance(cells.get(2)));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void bothColumnsSitOnTheShaftSide(Direction facing) {
    List<BlockPos> cells = LiftGateCells.cells(POS, facing);
    List<BlockPos> columns = LiftGateCells.columns(POS, facing);
    assertEquals(cells.get(0).offset(facing), columns.get(0));
    assertEquals(cells.get(2).offset(facing), columns.get(1));
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "SOUTH", "EAST", "WEST"})
  void theSecondCellResolvesBackToTheOrigin(Direction facing) {
    BlockPos second = LiftGateCells.cells(POS, facing).get(2);
    assertEquals(POS, LiftGateCells.origin(second, facing, true));
    assertEquals(POS, LiftGateCells.origin(POS, facing, false));
  }

  @ParameterizedTest(name = "facing {0}, hit {1},{2}")
  @CsvSource({
    "NORTH,0.8,0.5,false",
    "NORTH,0.2,0.5,true",
    "SOUTH,0.2,0.5,false",
    "SOUTH,0.8,0.5,true",
    "EAST,0.5,0.8,false",
    "EAST,0.5,0.2,true",
    "WEST,0.5,0.2,false",
    "WEST,0.5,0.8,true"
  })
  void aimingAtOneHalfGrowsTheGateThatWay(Direction facing, double x, double z, boolean second) {
    Vec3d hit = new Vec3d(POS.getX() + x, POS.getY(), POS.getZ() + z);
    assertEquals(second, LiftGateCells.placedAsSecond(POS, facing, hit));
    BlockPos origin = LiftGateCells.origin(POS, facing, second);
    assertTrue(LiftGateCells.cells(origin, facing).contains(POS));
  }
}
