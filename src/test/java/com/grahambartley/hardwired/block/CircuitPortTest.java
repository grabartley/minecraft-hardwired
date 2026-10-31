package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.circuit.PortMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CircuitPortTest {

  @ParameterizedTest(name = "{0} shows as {1}")
  @CsvSource({"UNUSED, unused", "INPUT, input", "OUTPUT, output"})
  void portsNameTheBlockstateValues(PortMode mode, String name) {
    assertEquals(name, CircuitPort.of(mode).asString());
  }
}
