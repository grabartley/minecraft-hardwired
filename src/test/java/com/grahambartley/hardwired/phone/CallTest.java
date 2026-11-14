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

  private static Call call(Long... called) {
    return new Call(CALLER, ALICE, List.of(called), Set.of(9L), 100);
  }

  @ParameterizedTest(name = "phone {0}")
  @CsvSource({"2,true", "3,true", "4,false", "1,false"})
  void onlyACalledPhoneRingsAndAnswers(long phone, boolean rings) {
    Call call = call(2L, 3L);
    assertEquals(rings, call.isRinging(phone));
    assertEquals(rings, call.answer(phone, BOB));
    assertEquals(rings, call.isConnected());
  }

  @ParameterizedTest(name = "second answer on {0}")
  @ValueSource(longs = {2, 3})
  void twoAnswersGiveOneAnswer(long second) {
    Call call = call(2L, 3L);
    assertTrue(call.answer(2L, BOB));
    assertFalse(call.answer(second, CAROL));
    assertEquals(List.of(ALICE, BOB), List.copyOf(call.members().values()));
  }

  @ParameterizedTest(name = "open wire {0}")
  @ValueSource(booleans = {true, false})
  void anotherPhoneOnTheNumberJoinsOnlyOnOpenWire(boolean open) {
    Call call = call(2L, 3L);
    assertFalse(call.join(3L, CAROL, open));
    call.answer(2L, BOB);
    assertEquals(open, call.join(3L, CAROL, open));
    assertFalse(call.join(3L, CAROL, open));
  }

  @ParameterizedTest(name = "leaving {0} ends {1}")
  @CsvSource({"1,true", "2,true", "4,false"})
  void aTwoWayCallEndsWhenEitherSideLeaves(long leaver, boolean ends) {
    Call call = call(2L);
    call.answer(2L, BOB);
    assertEquals(ends, call.leave(leaver));
  }

  @ParameterizedTest(name = "{0} left")
  @CsvSource({"1,false", "2,true"})
  void aPartyCallLastsWhileTwoRemain(int leavers, boolean ends) {
    Call call = call(2L, 3L);
    call.answer(2L, BOB);
    call.join(3L, CAROL, true);
    boolean ended = false;
    for (long phone = 1; phone <= leavers; phone++) {
      ended = call.leave(phone);
    }
    assertEquals(ends, ended);
  }

  @ParameterizedTest(name = "now {0}")
  @CsvSource({"100,false", "699,false", "700,true"})
  void anUnansweredCallRunsOut(long now, boolean over) {
    assertEquals(over, call(2L).unanswered(now, 600));
  }

  @ParameterizedTest(name = "phone {0}")
  @CsvSource({"1,true", "2,true", "5,false"})
  void aCallerHangingUpBeforeAnAnswerEndsIt(long phone, boolean involved) {
    Call call = call(2L);
    assertEquals(involved, call.involves(phone));
    assertEquals(phone == CALLER, call.leave(phone));
  }

  @ParameterizedTest(name = "dropping {0}")
  @CsvSource({"2,false", "3,false"})
  void aRingingPhoneThatGoesDarkStopsRingingAndTheLastOneEndsTheCall(long first, boolean ends) {
    Call call = call(2L, 3L);
    assertEquals(ends, call.stopRinging(first));
    assertFalse(call.isRinging(first));
    assertTrue(call.stopRinging(first == 2L ? 3L : 2L));
  }

  @ParameterizedTest(name = "phone {0}")
  @CsvSource({"2", "3"})
  void anAnsweredCallIgnoresRingingDrops(long phone) {
    Call call = call(2L, 3L);
    call.answer(2L, BOB);
    assertFalse(call.stopRinging(phone));
    assertTrue(call.called().contains(phone));
  }
}
