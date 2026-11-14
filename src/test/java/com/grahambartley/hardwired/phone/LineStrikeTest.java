package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LineStrikeTest {

  private static final Map<Long, Set<String>> NETWORKS =
      Map.of(1L, Set.of("struck"), 2L, Set.of("quiet"), 3L, Set.of("quiet"));

  @ParameterizedTest(name = "caller {0} answerer {1} connected {2} crackles {3}")
  @CsvSource({"1,2,true,true", "2,1,true,true", "2,3,true,false", "1,2,false,false"})
  void aStrikeCracklesEveryLiveCallTouchingItsGrid(
      long caller, long answerer, boolean connected, boolean crackles) {
    Call call = new Call(caller, UUID.randomUUID(), List.of(answerer), Set.of(), 0L);
    if (connected) {
      call.answer(answerer, UUID.randomUUID());
    }
    List<Call> struck = LineStrike.struck(List.of(call), NETWORKS::get, Set.of("struck"));
    assertEquals(crackles ? List.of(call) : List.of(), struck);
  }
}
