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

  @ParameterizedTest(name = "{0} powered {1}")
  @CsvSource({
    "RUNNING,true,OVERLOADED,RUNNING",
    "RUNNING,false,OVERLOADED,OVERLOADED",
    "RUNNING,false,NO_SOURCE,NO_SOURCE",
    "WIRE_OFF,false,NO_SOURCE,WIRE_OFF",
    "NO_TOWER,false,SOURCES_OFF,NO_TOWER",
    "RADIO_OFF,true,OUT_OF_FUEL,RADIO_OFF"
  })
  void aRunningDeviceWithNoPowerStopsWithItsGridsReason(
      DeviceStatus demand, boolean powered, DeviceStatus reason, DeviceStatus next) {
    assertEquals(next, ElectronicsRule.next(demand, powered, reason));
  }

  @ParameterizedTest(name = "beat at {0}, now {1}")
  @CsvSource({"100,100,false", "100,119,false", "100,120,true", "100,500,true", "-20,0,true"})
  void aBeatFallsOnceASecond(long beatAt, long now, boolean due) {
    assertEquals(due, ElectronicsRule.beatDue(beatAt, now));
  }

  private static Stream<Arguments> lightsOutCases() {
    DeviceStatus run = DeviceStatus.RUNNING;
    DeviceStatus out = DeviceStatus.OUT_OF_FUEL;
    return Stream.of(
        Arguments.of(run, out, List.of(out), true),
        Arguments.of(run, out, List.of(out, out, DeviceStatus.READY), true),
        Arguments.of(run, out, List.of(out, DeviceStatus.NO_RADIO), true),
        Arguments.of(run, out, List.of(out, run), false),
        Arguments.of(out, out, List.of(out), false),
        Arguments.of(run, DeviceStatus.WIRE_OFF, List.of(DeviceStatus.WIRE_OFF), false),
        Arguments.of(run, DeviceStatus.NO_SOURCE, List.of(DeviceStatus.NO_SOURCE), true));
  }

  @ParameterizedTest(name = "{0} to {1} with {2}")
  @MethodSource("lightsOutCases")
  void lightsOutNeedsTheLastRunningDeviceToRunDry(
      DeviceStatus before, DeviceStatus after, List<DeviceStatus> network, boolean earned) {
    assertEquals(earned, ElectronicsRule.lightsOut(before, after, () -> network));
  }

  @ParameterizedTest(name = "{0} to {1} reads the network: {2}")
  @CsvSource({
    "RUNNING,OUT_OF_FUEL,true",
    "RUNNING,RUNNING,false",
    "RUNNING,NO_SOURCE,true",
    "RUNNING,WIRE_OFF,false",
    "OUT_OF_FUEL,OUT_OF_FUEL,false",
    "READY,OUT_OF_FUEL,false"
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
