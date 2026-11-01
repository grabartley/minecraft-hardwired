package com.grahambartley.hardwired.radio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class CrackleSchedule<S> {

  private final Map<S, Long> next = new HashMap<>();

  public static <D, S> Map<S, List<D>> byStation(
      Collection<D> devices, Function<D, S> station, Predicate<D> receiving) {
    Map<S, List<D>> stations = new HashMap<>();
    for (D device : devices) {
      S tuned = station.apply(device);
      if (tuned != null && receiving.test(device)) {
        stations.computeIfAbsent(tuned, key -> new ArrayList<>()).add(device);
      }
    }
    return stations;
  }

  public <D> List<S> due(
      Map<S, List<D>> stations, ToDoubleFunction<D> strength, long now, DoubleSupplier roll) {
    next.keySet().retainAll(stations.keySet());
    List<S> firing = new ArrayList<>();
    stations.forEach(
        (station, tuned) -> {
          Long due = next.get(station);
          if (due != null && now >= due) {
            firing.add(station);
          }
          if (due == null || now >= due) {
            double strongest = tuned.stream().mapToDouble(strength).max().orElse(0);
            next.put(station, now + CrackleClock.gapTicks(strongest, roll.getAsDouble()));
          }
        });
    return firing;
  }

  public void clear() {
    next.clear();
  }
}
