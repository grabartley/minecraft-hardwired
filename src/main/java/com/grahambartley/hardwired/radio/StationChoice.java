package com.grahambartley.hardwired.radio;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class StationChoice {

  private static final Comparator<Reception> BEST_FIRST =
      Comparator.comparingDouble(Reception::strength)
          .reversed()
          .thenComparingLong(reception -> reception.station().onAirSince())
          .thenComparing(reception -> reception.station().desk());

  private StationChoice() {}

  public static Optional<Reception> best(Collection<Reception> heard) {
    return heard.stream().filter(reception -> reception.strength() > 0).min(BEST_FIRST);
  }
}
