package com.grahambartley.hardwired.phone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ExchangeLines {

  private ExchangeLines() {}

  public static boolean free(Collection<Call> calls, Set<Long> exchanges, int lines) {
    return exchanges.stream().allMatch(exchange -> inUse(calls, exchange) < lines);
  }

  public static Map<Long, Integer> lampsLit(Collection<Call> calls, int lamps) {
    Map<Long, Integer> lit = new HashMap<>();
    for (Call call : calls) {
      call.exchanges().forEach(exchange -> lit.merge(exchange, 1, Integer::sum));
    }
    lit.replaceAll((exchange, count) -> Math.min(count, lamps));
    return lit;
  }

  public static long inUse(Collection<Call> calls, long exchange) {
    return calls.stream().filter(call -> call.exchanges().contains(exchange)).count();
  }
}
