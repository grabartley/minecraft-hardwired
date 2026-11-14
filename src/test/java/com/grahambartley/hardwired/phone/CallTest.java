package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CallTest {

  private static final long CALLER = 1L;
  private static final UUID ALICE = new UUID(0, 1);
  private static final UUID BOB = new UUID(0, 2);
  private static final UUID CAROL = new UUID(0, 3);

  private static Call call(boolean joinable, Long... called) {
    return new Call(CALLER, ALICE, List.of(called), Set.of(9L), joinable, 100);
  }

  @ParameterizedTest(name = "phone {0}")
  @CsvSource({"2,true", "3,true", "4,false", "1,false"})
  void onlyACalledPhoneRingsAndAnswers(long phone, boolean rings) {
    Call call = call(true, 2L, 3L);
    assertEquals(rings, call.isRinging(phone));
    assertEquals(rings, call.answer(phone, BOB));
    assertEquals(rings, call.isConnected());
  }

  @ParameterizedTest(name = "second answer on {0}")
  @ValueSource(longs = {2, 3})
  void twoAnswersGiveOneAnswer(long second) {
    Call call = call(true, 2L, 3L);
    assertTrue(call.answer(2L, BOB));
    assertFalse(call.answer(second, CAROL));
    assertEquals(List.of(ALICE, BOB), List.copyOf(call.players()));
  }

  @ParameterizedTest(name = "joinable {0}")
  @ValueSource(booleans = {true, false})
  void anotherPhoneOnTheNumberJoinsOnlyOnOpenWire(boolean joinable) {
    Call call = call(joinable, 2L, 3L);
    assertFalse(call.join(3L, CAROL));
    call.answer(2L, BOB);
    assertEquals(joinable, call.join(3L, CAROL));
    assertFalse(call.join(3L, CAROL));
  }

  @ParameterizedTest(name = "leaving {0} ends {1}")
  @CsvSource({"1,true", "2,true", "4,false"})
  void aTwoWayCallEndsWhenEitherSideLeaves(long leaver, boolean ends) {
    Call call = call(true, 2L);
    call.answer(2L, BOB);
    assertEquals(ends, call.leave(leaver));
  }

  @ParameterizedTest(name = "{0} left")
  @CsvSource({"1,false", "2,true"})
  void aPartyCallLastsWhileTwoRemain(int leavers, boolean ends) {
    Call call = call(true, 2L, 3L);
    call.answer(2L, BOB);
    call.join(3L, CAROL);
    boolean ended = false;
    for (long phone = 1; phone <= leavers; phone++) {
      ended = call.leave(phone);
    }
    assertEquals(ends, ended);
  }

  @ParameterizedTest(name = "now {0}")
  @CsvSource({"100,false", "699,false", "700,true"})
  void anUnansweredCallRunsOut(long now, boolean over) {
    assertEquals(over, call(true, 2L).unanswered(now, 600));
  }

  @ParameterizedTest(name = "phone {0}")
  @CsvSource({"1,true", "2,true", "5,false"})
  void aCallerHangingUpBeforeAnAnswerEndsIt(long phone, boolean involved) {
    Call call = call(true, 2L);
    assertEquals(involved, call.involves(phone));
    assertEquals(phone == CALLER, call.leave(phone));
  }
}
