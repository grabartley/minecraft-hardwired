package com.grahambartley.hardwired.client.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.network.SpanViewPayload.SpanView;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientSpansTest {

  private static final BlockPos WEST = new BlockPos(0, 70, 0);
  private static final BlockPos EAST = new BlockPos(16, 70, 0);
  private static final BlockPos NORTH = new BlockPos(0, 70, -16);

  @ParameterizedTest(name = "colours {0} and {1}")
  @CsvSource({"3,5,0.28125,0.71875", "5,3,0.71875,0.28125"})
  void eachColourOnAPoleTakesItsOwnPlaceOnTheArm(
      int first, int second, double firstZ, double secondZ) {
    List<ClientSpans.Drawn> drawn =
        ClientSpans.lay(
            List.of(
                new SpanView(WEST.asLong(), EAST.asLong(), first, 0, false),
                new SpanView(WEST.asLong(), EAST.asLong(), second, 0, false)),
            top -> Direction.Axis.Z);
    assertEquals(firstZ, drawn.get(0).curve().from().z, 1e-9);
    assertEquals(secondZ, drawn.get(1).curve().from().z, 1e-9);
    assertEquals(SpanColours.rgb(first, 0, false), drawn.get(0).rgb());
  }

  @ParameterizedTest(name = "west arm {0}")
  @CsvSource({"X,0.71875,0.5", "Z,0.5,0.71875"})
  void eachEndUsesItsOwnPolesArmAndColours(Direction.Axis westArm, double westX, double westZ) {
    List<ClientSpans.Drawn> drawn =
        ClientSpans.lay(
            List.of(
                new SpanView(WEST.asLong(), EAST.asLong(), 7, 0, false),
                new SpanView(WEST.asLong(), NORTH.asLong(), 2, 0, false)),
            top -> top.equals(WEST) ? westArm : Direction.Axis.Z);
    SpanCurve toEast = drawn.get(0).curve();
    assertEquals(westX, toEast.from().x, 1e-9);
    assertEquals(westZ, toEast.from().z, 1e-9);
    assertEquals(16.5, toEast.to().x, 1e-9);
    assertEquals(0.5, toEast.to().z, 1e-9);
  }
}
