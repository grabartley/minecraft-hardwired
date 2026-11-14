package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExchangeLinesTest {

  private static final long LOCAL = 10L;
  private static final long FAR = 20L;

  private static List<Call> calls(int local, int between) {
    return IntStream.range(0, local + between)
        .mapToObj(
            i ->
                new Call(
                    i,
                    new UUID(0, i),
                    List.of(100L + i),
                    i < local ? Set.of(LOCAL) : Set.of(LOCAL, FAR),
                    true,
                    0))
        .toList();
  }

  @ParameterizedTest(name = "{0} local, {1} between, {2} lines")
  @CsvSource({
    "0,0,1,true",
    "3,0,4,true",
    "4,0,4,false",
    "2,2,4,false",
    "1,2,4,true",
    "1,1,2,false"
  })
  void aCallNeedsAFreeLineOnEveryExchangeItUses(int local, int between, int lines, boolean free) {
    List<Call> calls = calls(local, between);
    assertEquals(free, ExchangeLines.free(calls, Set.of(LOCAL), lines));
    assertEquals(free, ExchangeLines.free(calls, Set.of(LOCAL, FAR), lines));
  }
}
