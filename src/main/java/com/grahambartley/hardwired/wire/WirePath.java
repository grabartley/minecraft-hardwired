package com.grahambartley.hardwired.wire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class WirePath {

  private WirePath() {}

  public static List<WireFace> between(
      WireFace from, WireFace to, Predicate<WireFace> allowed, int maxVisited) {
    Map<WireFace, WireFace> cameFrom = new HashMap<>();
    Deque<WireFace> queue = new ArrayDeque<>();
    cameFrom.put(from, from);
    queue.add(from);
    while (!queue.isEmpty() && cameFrom.size() < maxVisited) {
      WireFace current = queue.poll();
      if (current.equals(to)) {
        return unwind(cameFrom, from, to);
      }
      for (WireFace next : WireTopology.neighbours(current)) {
        if (!cameFrom.containsKey(next) && allowed.test(next)) {
          cameFrom.put(next, current);
          queue.add(next);
        }
      }
    }
    return List.of(to);
  }

  private static List<WireFace> unwind(
      Map<WireFace, WireFace> cameFrom, WireFace from, WireFace to) {
    List<WireFace> path = new ArrayList<>();
    for (WireFace face = to; !face.equals(from); face = cameFrom.get(face)) {
      path.add(face);
    }
    Collections.reverse(path);
    return path;
  }
}
