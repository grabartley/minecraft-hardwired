package com.grahambartley.hardwired.power;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class LinkedGroups {

  private LinkedGroups() {}

  public static <T> Set<T> reach(
      Collection<T> starts, Function<T, ? extends Iterable<T>> neighbours) {
    Set<T> found = new LinkedHashSet<>(starts);
    Deque<T> queue = new ArrayDeque<>(found);
    while (!queue.isEmpty()) {
      for (T next : neighbours.apply(queue.poll())) {
        if (found.add(next)) {
          queue.add(next);
        }
      }
    }
    return found;
  }

  public static <T> List<Set<T>> of(
      Collection<T> nodes, Function<T, ? extends Iterable<T>> neighbours) {
    Set<T> left = new LinkedHashSet<>(nodes);
    List<Set<T>> groups = new ArrayList<>();
    while (!left.isEmpty()) {
      Set<T> group = reach(List.of(left.iterator().next()), neighbours);
      group.retainAll(left);
      left.removeAll(group);
      groups.add(group);
    }
    return groups;
  }
}
