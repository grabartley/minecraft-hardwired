package com.grahambartley.hardwired.phone;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PartyLines {

  public static final int MOST_PHONES = 4;

  private PartyLines() {}

  public static Map<Long, Integer> share(Map<Long, Integer> numbers, List<Long> chosen) {
    if (chosen.size() < 2 || !numbers.keySet().containsAll(chosen)) {
      return Map.of();
    }
    int shared = chosen.stream().mapToInt(numbers::get).min().orElseThrow();
    long already =
        numbers.entrySet().stream()
            .filter(entry -> entry.getValue() == shared && !chosen.contains(entry.getKey()))
            .count();
    if (chosen.size() + already > MOST_PHONES) {
      return Map.of();
    }
    Map<Long, Integer> changed = new HashMap<>();
    chosen.stream()
        .filter(phone -> numbers.get(phone) != shared)
        .forEach(phone -> changed.put(phone, shared));
    return changed;
  }

  public static Map<Long, Integer> part(
      Map<Long, Integer> numbers, Map<Long, Long> orders, List<Long> chosen) {
    Map<Long, Integer> changed = new HashMap<>();
    for (long phone : chosen) {
      Integer number = numbers.get(phone);
      if (number == null) {
        continue;
      }
      List<Long> sharers = sharers(numbers, number);
      boolean everyoneChosen = chosen.containsAll(sharers);
      long keeper = sharers.stream().min(Comparator.comparingLong(orders::get)).orElse(phone);
      if (sharers.size() > 1 && !(everyoneChosen && keeper == phone)) {
        changed.put(phone, PhoneNumbering.NONE);
      }
    }
    return changed;
  }

  private static List<Long> sharers(Map<Long, Integer> numbers, int number) {
    return numbers.entrySet().stream()
        .filter(entry -> entry.getValue() == number)
        .map(Map.Entry::getKey)
        .toList();
  }
}
