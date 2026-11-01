package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ElectronicsRuleTest {

  @ParameterizedTest(name = "{0} paid {1} cell {2}")
  @CsvSource({
    "RUNNING,true,true,RUNNING",
    "RUNNING,true,false,RUNNING",
    "RUNNING,false,true,NO_CHARGE",
    "RUNNING,false,false,NO_CELL",
    "WIRE_OFF,false,true,WIRE_OFF",
    "NO_TOWER,false,false,NO_TOWER",
    "RADIO_OFF,false,true,RADIO_OFF"
  })
  void aRunningDeviceThatCannotPayStopsWithAReason(
      DeviceStatus demand, boolean paid, boolean hasCell, DeviceStatus next) {
    assertEquals(next, ElectronicsRule.next(demand, paid, hasCell));
  }

  @ParameterizedTest(name = "paid at {0}, now {1}")
  @CsvSource({"100,100,true", "100,119,true", "100,120,false", "100,500,false", "-20,0,false"})
  void aSecondIsPaidOnceAndCoversItsBeat(long paidAt, long now, boolean covered) {
    assertEquals(covered, ElectronicsRule.beatPaid(paidAt, now));
  }

  @ParameterizedTest(name = "cost {0} cell {1} available {2}")
  @CsvSource({"1,true,1,READY", "1,true,0,NO_CHARGE", "1,false,0,NO_CELL", "0,false,0,READY"})
  void anIntercomIsReadyWhenItCanPayForALine(
      int cost, boolean hasCell, int available, DeviceStatus status) {
    assertEquals(status, ElectronicsRule.readiness(cost, hasCell, available));
  }

  private static Stream<Arguments> lightsOutCases() {
    DeviceStatus run = DeviceStatus.RUNNING;
    DeviceStatus out = DeviceStatus.NO_CHARGE;
    return Stream.of(
        Arguments.of(run, out, List.of(out), true),
        Arguments.of(run, out, List.of(out, out, DeviceStatus.READY), true),
        Arguments.of(run, out, List.of(out, DeviceStatus.NO_RADIO), true),
        Arguments.of(run, out, List.of(out, run), false),
        Arguments.of(out, out, List.of(out), false),
        Arguments.of(run, DeviceStatus.WIRE_OFF, List.of(DeviceStatus.WIRE_OFF), false),
        Arguments.of(run, DeviceStatus.NO_CELL, List.of(DeviceStatus.NO_CELL), false));
  }

  @ParameterizedTest(name = "{0} to {1} with {2}")
  @MethodSource("lightsOutCases")
  void lightsOutNeedsTheLastRunningDeviceToRunDry(
      DeviceStatus before, DeviceStatus after, List<DeviceStatus> network, boolean earned) {
    assertEquals(earned, ElectronicsRule.lightsOut(before, after, () -> network));
  }

  @ParameterizedTest(name = "{0} to {1} reads the network: {2}")
  @CsvSource({
    "RUNNING,NO_CHARGE,true",
    "RUNNING,RUNNING,false",
    "RUNNING,NO_CELL,false",
    "NO_CHARGE,NO_CHARGE,false",
    "READY,NO_CHARGE,false"
  })
  void theNetworkIsOnlyReadWhenADeviceHasJustRunDry(
      DeviceStatus before, DeviceStatus after, boolean read) {
    AtomicBoolean asked = new AtomicBoolean();
    ElectronicsRule.lightsOut(
        before,
        after,
        () -> {
          asked.set(true);
          return List.of();
        });
    assertEquals(read, asked.get());
  }
}
