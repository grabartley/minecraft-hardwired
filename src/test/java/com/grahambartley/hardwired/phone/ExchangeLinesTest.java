package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
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

  @ParameterizedTest(name = "{0} local, {1} between, {2} lamps")
  @CsvSource({"0,0,12,0,0", "3,0,12,3,0", "2,1,12,3,1", "10,5,12,12,5", "4,0,2,2,0"})
  void eachCallLightsALampOnEveryExchangeItUsesUpToTheLimit(
      int local, int between, int lamps, int localLit, int farLit) {
    Map<Long, Integer> lit = ExchangeLines.lampsLit(calls(local, between), lamps);
    assertEquals(localLit, lit.getOrDefault(LOCAL, 0));
    assertEquals(farLit, lit.getOrDefault(FAR, 0));
  }
}
