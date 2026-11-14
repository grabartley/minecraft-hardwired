package com.grahambartley.hardwired.line;

import java.util.ArrayList;
import java.util.List;

public final class SpanRules {

  public static final int MOST_SPANS = 8;

  public enum Finish {
    CANCEL,
    TOO_FAR,
    POLE_FULL,
    ALREADY_STRUNG,
    NETWORK_FULL,
    NEEDS_WIRE,
    STRUNG
  }

  public record Attempt(
      boolean samePole,
      double length,
      int maxBlocks,
      int spansAtStart,
      int spansAtEnd,
      boolean alreadyStrung,
      int sizeAfter,
      int maxSize,
      int wireHeld,
      int cost,
      boolean creative) {}

  private SpanRules() {}

  public static int cost(double length, int blocksPerWire) {
    return Math.max(1, (int) Math.ceil(length / blocksPerWire));
  }

  public static boolean reaches(double length, int maxBlocks) {
    return length > 0 && length <= maxBlocks;
  }

  public static List<Integer> stackSizes(int count, int maxStack) {
    List<Integer> sizes = new ArrayList<>();
    for (int left = count; left > 0; left -= maxStack) {
      sizes.add(Math.min(left, maxStack));
    }
    return List.copyOf(sizes);
  }

  public static boolean hasRoom(int spansOnPole) {
    return spansOnPole < MOST_SPANS;
  }

  public static Finish finish(Attempt attempt) {
    if (attempt.samePole()) {
      return Finish.CANCEL;
    }
    if (!reaches(attempt.length(), attempt.maxBlocks())) {
      return Finish.TOO_FAR;
    }
    if (!hasRoom(attempt.spansAtStart()) || !hasRoom(attempt.spansAtEnd())) {
      return Finish.POLE_FULL;
    }
    if (attempt.alreadyStrung()) {
      return Finish.ALREADY_STRUNG;
    }
    if (attempt.sizeAfter() > attempt.maxSize()) {
      return Finish.NETWORK_FULL;
    }
    return attempt.creative() || attempt.wireHeld() >= attempt.cost()
        ? Finish.STRUNG
        : Finish.NEEDS_WIRE;
  }
}
