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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CircuitBenchTest {

  private static final int NOT = Design.partNode(0);
  private static final Design INVERTER =
      new Design(
          List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
          List.of(new Part(PartType.NOT, 1, 1, 0)),
          List.of(new Line(0, NOT, 0), new Line(NOT, 1, 0)));

  @Test
  void aNewBenchShowsTheFirstStepAtOnce() {
    CircuitBench bench = new CircuitBench(INVERTER);
    assertTrue(bench.isOn(NOT));
    assertTrue(bench.isSideOn(Side.EAST));
  }

  @ParameterizedTest(name = "after {0} ticks the output is {1}")
  @CsvSource({"0, true", "1, true", "2, false", "3, false"})
  void theBenchStepsAtTheBlocksRate(int ticks, boolean output) {
    CircuitBench bench = new CircuitBench(INVERTER);
    bench.toggle(Side.NORTH);
    for (int tick = 0; tick < ticks; tick++) {
      bench.tick();
    }
    assertEquals(output, bench.isSideOn(Side.EAST));
  }

  @Test
  void onlyInputSidesToggle() {
    CircuitBench bench = new CircuitBench(INVERTER);
    bench.toggle(Side.SOUTH);
    assertFalse(bench.isSideOn(Side.SOUTH));
    bench.toggle(Side.NORTH);
    assertTrue(bench.isSideOn(Side.NORTH));
    bench.toggle(Side.NORTH);
    assertFalse(bench.isSideOn(Side.NORTH));
  }
}
