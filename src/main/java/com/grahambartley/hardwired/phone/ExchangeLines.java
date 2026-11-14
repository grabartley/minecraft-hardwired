package com.grahambartley.hardwired.phone;

import java.util.Collection;
import java.util.Set;

public final class ExchangeLines {

  private ExchangeLines() {}

  public static boolean free(Collection<Call> calls, Set<Long> exchanges, int lines) {
    return exchanges.stream().allMatch(exchange -> inUse(calls, exchange) < lines);
  }

  public static long inUse(Collection<Call> calls, long exchange) {
    return calls.stream().filter(call -> call.exchanges().contains(exchange)).count();
  }
}
