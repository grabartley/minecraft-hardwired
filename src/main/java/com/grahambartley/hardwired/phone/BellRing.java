package com.grahambartley.hardwired.phone;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public final class BellRing {

  private BellRing() {}

  public static boolean rings(
      Collection<Call> calls,
      Set<Long> touched,
      Function<Long, Collection<Long>> cellsOf,
      long now) {
    return calls.stream()
        .filter(call -> !call.isConnected())
        .filter(call -> RingCadence.ringing(call.ringingFor(now), call.isPartyLine()))
        .anyMatch(
            call ->
                call.called().stream()
                    .filter(call::isRinging)
                    .anyMatch(phone -> cellsOf.apply(phone).stream().anyMatch(touched::contains)));
  }
}
