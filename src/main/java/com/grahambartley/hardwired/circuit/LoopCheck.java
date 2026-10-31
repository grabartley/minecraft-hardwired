package com.grahambartley.hardwired.circuit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public final class LoopCheck {

  private LoopCheck() {}

  public static boolean hasLoop(Design design) {
    return gateOrder(design).isEmpty();
  }

  public static Optional<int[]> gateOrder(Design design) {
    int nodes = design.nodes();
    int[] waiting = new int[nodes];
    List<List<Integer>> feeds = new ArrayList<>(nodes);
    for (int node = 0; node < nodes; node++) {
      feeds.add(new ArrayList<>());
    }
    for (Line line : design.lines()) {
      if (isGate(design, line.from()) && isGate(design, line.to())) {
        feeds.get(line.from()).add(line.to());
        waiting[line.to()]++;
      }
    }
    Deque<Integer> ready = new ArrayDeque<>();
    int gates = 0;
    for (int node = 0; node < nodes; node++) {
      if (isGate(design, node)) {
        gates++;
        if (waiting[node] == 0) {
          ready.add(node);
        }
      }
    }
    int[] order = new int[gates];
    int placed = 0;
    while (!ready.isEmpty()) {
      int node = ready.poll();
      order[placed++] = node;
      for (int next : feeds.get(node)) {
        if (--waiting[next] == 0) {
          ready.add(next);
        }
      }
    }
    return placed == gates ? Optional.of(Arrays.copyOf(order, gates)) : Optional.empty();
  }

  private static boolean isGate(Design design, int node) {
    return !design.isSide(node) && !design.partAt(node).type().isDelay();
  }
}
