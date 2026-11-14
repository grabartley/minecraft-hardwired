package com.grahambartley.hardwired.phone;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

public final class PhoneNumbering {

  public static final int NONE = -1;
  public static final int FIRST_DIGIT = 1;
  public static final int LAST_DIGIT = 9;
  public static final int LINES_PER_EXCHANGE = 100;

  public record Exchange(long pos, long order, int digit, boolean allowed) {}

  public record Phone(long pos, long order, int number, long exchange, boolean allowed) {}

  public record Assignment(int number, long exchange) {

    public static final Assignment UNNUMBERED = new Assignment(NONE, 0L);
  }

  public record Result(Map<Long, Integer> digits, Map<Long, Assignment> phones) {}

  private PhoneNumbering() {}

  public static int number(int digit, int line) {
    return digit * LINES_PER_EXCHANGE + line;
  }

  public static int lineOf(int number) {
    return number % LINES_PER_EXCHANGE;
  }

  public static Result assign(List<Exchange> exchanges, List<Phone> phones) {
    Map<Long, Integer> digits = assignDigits(exchanges);
    Map<Long, Assignment> numbers = assignNumbers(phones, digits);
    return new Result(digits, numbers);
  }

  private static Map<Long, Integer> assignDigits(List<Exchange> exchanges) {
    List<Exchange> byOrder =
        exchanges.stream().sorted(Comparator.comparingLong(Exchange::order)).toList();
    Map<Long, Integer> digits = new HashMap<>();
    Set<Integer> used = new HashSet<>();
    for (Exchange exchange : byOrder) {
      if (exchange.allowed() && isDigit(exchange.digit()) && used.add(exchange.digit())) {
        digits.put(exchange.pos(), exchange.digit());
      }
    }
    for (Exchange exchange : byOrder) {
      if (!digits.containsKey(exchange.pos())) {
        digits.put(exchange.pos(), exchange.allowed() ? lowestFreeDigit(used) : NONE);
      }
    }
    return digits;
  }

  private static int lowestFreeDigit(Set<Integer> used) {
    for (int digit = FIRST_DIGIT; digit <= LAST_DIGIT; digit++) {
      if (used.add(digit)) {
        return digit;
      }
    }
    return NONE;
  }

  private static Map<Long, Assignment> assignNumbers(
      List<Phone> phones, Map<Long, Integer> digits) {
    List<Phone> byOrder = phones.stream().sorted(Comparator.comparingLong(Phone::order)).toList();
    Map<Long, Assignment> numbers = new HashMap<>();
    Map<Long, Map<Integer, Integer>> taken = new HashMap<>();
    for (Phone phone : byOrder) {
      Assignment kept = keep(phone, digits, taken);
      if (kept != null) {
        numbers.put(phone.pos(), kept);
      }
    }
    for (Phone phone : byOrder) {
      if (!numbers.containsKey(phone.pos())) {
        numbers.put(
            phone.pos(),
            phone.allowed() ? nearestFree(phone, digits, taken) : Assignment.UNNUMBERED);
      }
    }
    return numbers;
  }

  private static Assignment keep(
      Phone phone, Map<Long, Integer> digits, Map<Long, Map<Integer, Integer>> taken) {
    int digit = digits.getOrDefault(phone.exchange(), NONE);
    if (phone.number() == NONE || !isDigit(digit)) {
      return null;
    }
    int line = lineOf(phone.number());
    Map<Integer, Integer> lines = taken.computeIfAbsent(phone.exchange(), key -> new HashMap<>());
    Integer holder = lines.putIfAbsent(line, phone.number());
    if (holder != null && holder != phone.number()) {
      return null;
    }
    return new Assignment(number(digit, line), phone.exchange());
  }

  private static Assignment nearestFree(
      Phone phone, Map<Long, Integer> digits, Map<Long, Map<Integer, Integer>> taken) {
    BlockPos at = BlockPos.fromLong(phone.pos());
    List<Long> nearest =
        digits.entrySet().stream()
            .filter(entry -> isDigit(entry.getValue()))
            .map(Map.Entry::getKey)
            .sorted(
                Comparator.comparingDouble(
                        (Long exchange) -> at.getSquaredDistance(BlockPos.fromLong(exchange)))
                    .thenComparingLong(exchange -> exchange))
            .toList();
    for (long exchange : nearest) {
      Map<Integer, Integer> lines = taken.computeIfAbsent(exchange, key -> new HashMap<>());
      for (int line = 0; line < LINES_PER_EXCHANGE; line++) {
        if (!lines.containsKey(line)) {
          int number = number(digits.get(exchange), line);
          lines.put(line, number);
          return new Assignment(number, exchange);
        }
      }
    }
    return Assignment.UNNUMBERED;
  }

  private static boolean isDigit(int digit) {
    return digit >= FIRST_DIGIT && digit <= LAST_DIGIT;
  }
}
