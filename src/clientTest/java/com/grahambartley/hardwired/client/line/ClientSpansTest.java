package com.grahambartley.hardwired.client.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.network.SpanViewPayload.SpanView;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientSpansTest {

  private static final long WEST = new BlockPos(0, 70, 0).asLong();
  private static final long EAST = new BlockPos(16, 70, 0).asLong();

  @ParameterizedTest(name = "colours {0} and {1}")
  @CsvSource({"3,5,0.59375,0.40625", "5,3,0.40625,0.59375", "4,4,0.5,0.5"})
  void eachColourOnAPoleTakesItsOwnLane(int first, int second, double firstZ, double secondZ) {
    List<ClientSpans.Drawn> drawn =
        ClientSpans.lay(
            List.of(
                new SpanView(WEST, EAST, first, 0, false),
                new SpanView(WEST, EAST, second, 0, false)));
    assertEquals(firstZ, drawn.get(0).curve().from().z, 1e-9);
    assertEquals(secondZ, drawn.get(1).curve().from().z, 1e-9);
    assertEquals(SpanColours.rgb(first, 0, false), drawn.get(0).rgb());
  }
}
