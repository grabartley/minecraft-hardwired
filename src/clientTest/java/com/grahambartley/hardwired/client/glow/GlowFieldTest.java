package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.grahambartley.hardwired.light.Glow;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowFieldTest {

  private static final float EPSILON = 0.001F;
  private static final int RED = 0xFF0000;
  private static final int BLUE = 0x0000FF;

  private static GlowField field(Predicate<BlockPos> solid) {
    return new GlowField(
        solid, (from, side) -> solid.test(from.offset(side)) ? Glow.MAX_LEVEL + 1 : 1);
  }

  @ParameterizedTest(name = "cost {0} per block leaves {2}/15 at {1} blocks")
  @CsvSource({"1,3,12", "2,3,9", "3,3,6"})
  void thickerBlocksDimLightFaster(int cost, int distance, int level) {
    GlowField field = new GlowField(pos -> false, (from, side) -> cost);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] cell = field.at(new BlockPos(distance, 0, 0).asLong());
    assertEquals(level / 15F, cell[GlowField.WEIGHT], EPSILON);
  }

  @Test
  void oneWaySurfaceStopsLightGoingThroughIt() {
    GlowField field =
        new GlowField(
            pos -> false,
            (from, side) -> from.getY() == 0 && side == Direction.UP ? Glow.MAX_LEVEL + 1 : 1);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    assertNull(field.at(BlockPos.ORIGIN.up().asLong()));
    assertEquals(14 / 15F, field.at(BlockPos.ORIGIN.down().asLong())[GlowField.WEIGHT], EPSILON);
  }

  @Test
  void cheaperWayRoundWinsOverACostlyShortcut() {
    GlowField field =
        new GlowField(
            pos -> false,
            (from, side) -> from.equals(BlockPos.ORIGIN) && side == Direction.EAST ? 10 : 1);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] cell = field.at(BlockPos.ORIGIN.east().asLong());
    assertEquals(12 / 15F, cell[GlowField.WEIGHT], EPSILON);
  }

  @ParameterizedTest(name = "{0} blocks away is weight {1}/15")
  @CsvSource({"0,15", "1,14", "7,8", "14,1"})
  void lightFallsOneLevelPerBlock(int distance, int level) {
    GlowField field = field(pos -> false);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] cell = field.at(new BlockPos(distance, 0, 0).asLong());
    assertEquals(level / 15F, cell[GlowField.WEIGHT], EPSILON);
    assertEquals(level / 15F, cell[GlowField.RED], EPSILON);
    assertEquals(0F, cell[GlowField.BLUE], EPSILON);
  }

  @ParameterizedTest(name = "level {0} stops after {1} blocks")
  @CsvSource({"15,15", "5,5", "1,1"})
  void lightStopsWhereItRunsOut(int level, int blocks) {
    GlowField field = field(pos -> false);
    field.add(new Glow(BlockPos.ORIGIN, level, RED));
    assertNull(field.at(new BlockPos(blocks, 0, 0).asLong()));
  }

  @Test
  void wallStopsLight() {
    GlowField field = field(pos -> pos.getX() == 2);
    field.add(new Glow(BlockPos.ORIGIN, 3, RED));
    assertNull(field.at(new BlockPos(2, 0, 0).asLong()));
    assertNull(field.at(new BlockPos(3, 0, 0).asLong()));
  }

  @Test
  void lightGoesAroundACorner() {
    GlowField field = field(pos -> pos.getX() == 1 && pos.getY() == 0 && pos.getZ() == 0);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] behind = field.at(new BlockPos(2, 0, 0).asLong());
    assertEquals(11 / 15F, behind[GlowField.WEIGHT], EPSILON);
  }

  @Test
  void overlappingLightsAddUp() {
    GlowField field = field(pos -> false);
    field.add(new Glow(new BlockPos(-3, 0, 0), 15, RED));
    field.add(new Glow(new BlockPos(3, 0, 0), 15, BLUE));
    float[] middle = field.at(BlockPos.ORIGIN.asLong());
    assertEquals(12 / 15F, middle[GlowField.RED], EPSILON);
    assertEquals(12 / 15F, middle[GlowField.BLUE], EPSILON);
    assertEquals(24 / 15F, middle[GlowField.WEIGHT], EPSILON);
  }

  @ParameterizedTest(name = "halfway between cells {0} and {1} blocks away is the mean")
  @CsvSource({"1,2", "3,4"})
  void sampleBlendsNeighbouringCells(int near, int far) {
    GlowField field = field(pos -> false);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] nearCell = field.at(new BlockPos(near, 0, 0).asLong());
    float[] farCell = field.at(new BlockPos(far, 0, 0).asLong());
    float[] sample = field.sample(far, 0.5D, 0.5D);
    assertEquals(
        (nearCell[GlowField.WEIGHT] + farCell[GlowField.WEIGHT]) / 2F,
        sample[GlowField.WEIGHT],
        EPSILON);
  }

  @Test
  void sampleIgnoresBlockedCells() {
    GlowField field = field(pos -> pos.getX() >= 1);
    field.add(new Glow(BlockPos.ORIGIN, 15, RED));
    float[] sample = field.sample(1D, 0.5D, 0.5D);
    assertEquals(1F, sample[GlowField.WEIGHT], EPSILON);
  }

  @Test
  void sampleInsideRockIsDark() {
    GlowField field = field(pos -> true);
    assertEquals(0F, field.sample(0.5D, 0.5D, 0.5D)[GlowField.WEIGHT], EPSILON);
  }
}
