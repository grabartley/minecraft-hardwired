package com.grahambartley.hardwired.phone;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class LineStrike {

  private LineStrike() {}

  public static <N> List<Call> struck(
      Collection<Call> calls, Function<Long, Set<N>> networksOf, Set<N> grid) {
    return calls.stream()
        .filter(Call::isConnected)
        .filter(
            call ->
                call.members().keySet().stream()
                    .anyMatch(phone -> !Collections.disjoint(networksOf.apply(phone), grid)))
        .toList();
  }
}
