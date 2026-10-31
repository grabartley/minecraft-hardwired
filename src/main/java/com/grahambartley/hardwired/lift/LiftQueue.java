package com.grahambartley.hardwired.lift;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LiftQueue {

  public static final int MAX_CALLS = 64;

  private final Deque<Integer> levels = new ArrayDeque<>();

  public boolean add(int level) {
    if (levels.size() >= MAX_CALLS || levels.contains(level)) {
      return false;
    }
    levels.addLast(level);
    return true;
  }

  public int poll() {
    return levels.removeFirst();
  }

  public boolean isEmpty() {
    return levels.isEmpty();
  }

  public void clear() {
    levels.clear();
  }

  public int[] toArray() {
    return levels.stream().mapToInt(Integer::intValue).toArray();
  }

  public void load(int[] saved) {
    levels.clear();
    for (int level : saved) {
      add(level);
    }
  }
}
