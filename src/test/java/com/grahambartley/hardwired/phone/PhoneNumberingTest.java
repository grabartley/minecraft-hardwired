package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.phone.PhoneNumbering.Exchange;
import com.grahambartley.hardwired.phone.PhoneNumbering.Phone;
import com.grahambartley.hardwired.phone.PhoneNumbering.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberingTest {

  private static final int NONE = PhoneNumbering.NONE;

  private static long at(int x) {
    return new BlockPos(x, 64, 0).asLong();
  }

  private static Exchange exchange(int x, long order, int digit) {
    return new Exchange(at(x), order, digit, true);
  }

  private static Phone phone(int x, long order, int number, int exchangeX) {
    return new Phone(at(x), order, number, at(exchangeX), true);
  }

  @ParameterizedTest(name = "{0} exchanges")
  @ValueSource(ints = {1, 5, 9, 10, 12})
  void exchangesTakeTheLowestFreeDigitUntilTheNetworkIsFull(int count) {
    List<Exchange> exchanges =
        IntStream.range(0, count).mapToObj(i -> exchange(i * 10, i, NONE)).toList();
    Result result = PhoneNumbering.assign(exchanges, List.of());
    for (int i = 0; i < count; i++) {
      int expected = i < PhoneNumbering.LAST_DIGIT ? i + 1 : NONE;
      assertEquals(expected, result.digits().get(at(i * 10)));
    }
  }

  @ParameterizedTest(name = "newer listed first {0}")
  @ValueSource(booleans = {true, false})
  void theLaterExchangeGivesWayWhenTwoShareADigit(boolean newerFirst) {
    Exchange first = exchange(0, 1, 3);
    Exchange second = exchange(50, 2, 3);
    Result result =
        PhoneNumbering.assign(
            newerFirst ? List.of(second, first) : List.of(first, second), List.of());
    assertEquals(3, result.digits().get(at(0)));
    assertEquals(1, result.digits().get(at(50)));
  }

  @ParameterizedTest(name = "phone at {0} joins {1}")
  @CsvSource({"5,0,100", "45,50,200", "24,0,100", "26,50,200"})
  void aNewPhoneJoinsTheNearestExchange(int phoneX, int exchangeX, int number) {
    Result result =
        PhoneNumbering.assign(
            List.of(exchange(0, 0, 1), exchange(50, 1, 2)), List.of(phone(phoneX, 0, NONE, 0)));
    assertEquals(number, result.phones().get(at(phoneX)).number());
    assertEquals(at(exchangeX), result.phones().get(at(phoneX)).exchange());
  }

  @ParameterizedTest(name = "kept line {0}")
  @ValueSource(ints = {105, 142, 199})
  void aPhoneKeepsItsNumberWhenLowerLinesFree(int number) {
    Result result =
        PhoneNumbering.assign(
            List.of(exchange(0, 0, 1)), List.of(phone(3, 0, number, 0), phone(4, 1, NONE, 0)));
    assertEquals(number, result.phones().get(at(3)).number());
    assertEquals(100, result.phones().get(at(4)).number());
  }

  @ParameterizedTest(name = "new digit {0}")
  @ValueSource(ints = {2, 7})
  void aPartyLineKeepsSharingWhenItsExchangeTakesANewDigit(int digit) {
    Exchange moved = new Exchange(at(0), 5, digit, true);
    Exchange keeper = exchange(90, 0, 1);
    Result result =
        PhoneNumbering.assign(
            List.of(moved, keeper),
            List.of(phone(1, 0, 112, 0), phone(2, 1, 112, 0), phone(3, 2, 113, 0)));
    int shared = PhoneNumbering.number(digit, 12);
    assertEquals(shared, result.phones().get(at(1)).number());
    assertEquals(shared, result.phones().get(at(2)).number());
    assertEquals(PhoneNumbering.number(digit, 13), result.phones().get(at(3)).number());
  }

  @ParameterizedTest(name = "old number {0}")
  @ValueSource(ints = {105, 100, 250})
  void aPhoneWhoseExchangeIsGoneTakesTheLowestFreeLineAtTheNearestOne(int oldNumber) {
    Phone kept = phone(1, 0, 105, 0);
    Phone orphan = new Phone(at(2), 1, oldNumber, at(70), true);
    Result result = PhoneNumbering.assign(List.of(exchange(0, 0, 1)), List.of(kept, orphan));
    assertEquals(105, result.phones().get(at(1)).number());
    assertEquals(100, result.phones().get(at(2)).number());
    assertEquals(at(0), result.phones().get(at(2)).exchange());
  }

  @ParameterizedTest(name = "old number {0}")
  @ValueSource(ints = {105, 142})
  void phonesWithOneNumberUnderOneExchangeStayAPartyLine(int number) {
    Result result =
        PhoneNumbering.assign(
            List.of(exchange(0, 0, 1)), List.of(phone(1, 0, number, 0), phone(2, 1, number, 0)));
    assertEquals(number, result.phones().get(at(1)).number());
    assertEquals(number, result.phones().get(at(2)).number());
  }

  @ParameterizedTest(name = "allowed {0}")
  @ValueSource(booleans = {true, false})
  void aStrangersDevicesGetNoNumber(boolean allowed) {
    Result result =
        PhoneNumbering.assign(
            List.of(new Exchange(at(0), 0, NONE, allowed)),
            List.of(new Phone(at(1), 0, NONE, 0L, true), new Phone(at(2), 1, NONE, 0L, allowed)));
    assertEquals(allowed ? 1 : NONE, result.digits().get(at(0)));
    assertEquals(allowed ? 100 : NONE, result.phones().get(at(1)).number());
    assertEquals(allowed ? 101 : NONE, result.phones().get(at(2)).number());
  }

  @ParameterizedTest(name = "{0} exchanges")
  @CsvSource({"1,false", "2,true"})
  void aFullExchangeSendsTheNextPhoneFurtherOrLeavesItUnnumbered(int count, boolean numbered) {
    List<Exchange> exchanges = new ArrayList<>(List.of(exchange(0, 0, 1)));
    if (count > 1) {
      exchanges.add(exchange(500, 1, 2));
    }
    List<Phone> phones =
        new ArrayList<>(
            IntStream.range(0, PhoneNumbering.LINES_PER_EXCHANGE)
                .mapToObj(i -> phone(i + 1, i, PhoneNumbering.number(1, i), 0))
                .toList());
    phones.add(phone(200, 1000, NONE, 0));
    Result result = PhoneNumbering.assign(exchanges, phones);
    assertEquals(numbered ? 200 : NONE, result.phones().get(at(200)).number());
  }

  @ParameterizedTest(name = "number {0}")
  @CsvSource({"100,1,0", "342,3,42", "999,9,99"})
  void aNumberSplitsIntoDigitAndLine(int number, int digit, int line) {
    assertEquals(digit, PhoneNumbering.digitOf(number));
    assertEquals(line, PhoneNumbering.lineOf(number));
    assertEquals(number, PhoneNumbering.number(digit, line));
  }
}
