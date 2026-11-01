package com.grahambartley.hardwired.radio;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class StreamPicker {

  public static final int MOST_STREAMS = 6;

  public record Candidate<T>(T source, double distance, double hearing) {}

  private StreamPicker() {}

  public static <T> List<T> nearest(Collection<Candidate<T>> candidates, int limit) {
    return candidates.stream()
        .filter(candidate -> candidate.distance() <= candidate.hearing())
        .sorted(Comparator.comparingDouble(Candidate::distance))
        .limit(limit)
        .map(Candidate::source)
        .toList();
  }
}
