package com.grahambartley.hardwired.radio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StreamShare {

  public record Wants<T>(T device, boolean voice, boolean song) {}

  public record Granted<T>(Set<T> voices, Set<T> songs) {}

  private StreamShare() {}

  public static <T> Granted<T> share(List<Wants<T>> nearestFirst, int limit) {
    Set<T> voices = new HashSet<>();
    Set<T> songs = new HashSet<>();
    for (Wants<T> wants : nearestFirst) {
      if (wants.voice() && voices.size() < limit) {
        voices.add(wants.device());
      }
    }
    for (Wants<T> wants : nearestFirst) {
      if (wants.song() && voices.size() + songs.size() < limit) {
        songs.add(wants.device());
      }
    }
    return new Granted<>(voices, songs);
  }
}
