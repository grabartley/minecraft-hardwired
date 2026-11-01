package com.grahambartley.hardwired.radio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CrackleScheduleTest {

  private record Device(String station, double strength, boolean running) {}

  private static final double MEAN_ROLL = 0.5;

  private static Map<String, List<Device>> tuned(Device... devices) {
    return CrackleSchedule.byStation(List.of(devices), Device::station, Device::running);
  }

  private static List<String> due(
      CrackleSchedule<String> schedule, Map<String, List<Device>> stations, long now) {
    return schedule.due(stations, Device::strength, now, () -> MEAN_ROLL);
  }

  @Test
  void devicesOnOneStationShareOneEntry() {
    Device radio = new Device("a", 1, true);
    Device speaker = new Device("a", 1, true);
    assertEquals(Map.of("a", List.of(radio, speaker)), tuned(radio, speaker));
  }

  @Test
  void stoppedAndUntunedDevicesAreLeftOut() {
    Device live = new Device("a", 1, true);
    assertEquals(
        Map.of("a", List.of(live)),
        tuned(live, new Device("a", 1, false), new Device(null, 1, true)));
  }

  @Test
  void aNewStationWaitsForItsFirstGap() {
    assertEquals(List.of(), due(new CrackleSchedule<>(), tuned(new Device("a", 1, true)), 0));
  }

  @ParameterizedTest(name = "{0} speakers still crackle once")
  @CsvSource({"1", "2", "12"})
  void moreSpeakersDoNotMeanMoreCrackle(int speakers) {
    Device[] devices = new Device[speakers];
    Arrays.fill(devices, new Device("a", 1, true));
    Map<String, List<Device>> stations = tuned(devices);
    CrackleSchedule<String> schedule = new CrackleSchedule<>();
    int firings = 0;
    for (long tick = 0; tick <= 1200; tick++) {
      firings += due(schedule, stations, tick).size();
    }
    assertEquals(2, firings);
  }

  @Test
  void stationsKeepTheirOwnClocks() {
    Map<String, List<Device>> both = tuned(new Device("a", 1, true), new Device("b", 0, true));
    CrackleSchedule<String> schedule = new CrackleSchedule<>();
    due(schedule, both, 0);
    assertEquals(List.of("b"), due(schedule, both, 100));
    assertEquals(List.of("b"), due(schedule, both, 200));
    assertEquals(Set.of("a", "b"), Set.copyOf(due(schedule, both, 600)));
  }

  @Test
  void theStrongestDeviceSetsThePace() {
    Map<String, List<Device>> mixed = tuned(new Device("a", 0, true), new Device("a", 1, true));
    CrackleSchedule<String> schedule = new CrackleSchedule<>();
    due(schedule, mixed, 0);
    assertEquals(List.of(), due(schedule, mixed, 599));
    assertEquals(List.of("a"), due(schedule, mixed, 600));
  }

  @Test
  void aStationThatGoesAwayStartsAfresh() {
    Map<String, List<Device>> stations = tuned(new Device("a", 1, true));
    CrackleSchedule<String> schedule = new CrackleSchedule<>();
    due(schedule, stations, 0);
    due(schedule, Map.of(), 1);
    assertEquals(List.of(), due(schedule, stations, 600));
  }
}
