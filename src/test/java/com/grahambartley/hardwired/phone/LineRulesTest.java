package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LineRulesTest {

  private static Optional<Boolean> held(String holder) {
    return switch (holder) {
      case "me" -> Optional.of(true);
      case "other" -> Optional.of(false);
      default -> Optional.empty();
    };
  }

  @ParameterizedTest(name = "ringing {0} held by {1} live call {2}")
  @CsvSource({
    "true,nobody,false,ANSWER",
    "true,other,true,ANSWER",
    "false,me,false,RESUME",
    "false,other,true,BUSY",
    "false,nobody,true,JOIN",
    "false,nobody,false,LIFT"
  })
  void pickingUpAnswersResumesJoinsOrLifts(
      boolean ringing, String holder, boolean liveCall, LineRules.PickUp expected) {
    assertEquals(expected, LineRules.pickUp(ringing, held(holder), liveCall));
  }

  @ParameterizedTest(name = "{8}")
  @CsvSource({
    "false,true,true,false,false,true,true,true,LINE_DEAD",
    "true,false,true,false,false,true,true,true,TOO_FAST",
    "true,true,false,false,false,false,true,true,NO_SUCH_NUMBER",
    "true,true,true,true,false,true,true,true,BUSY",
    "true,true,true,false,true,true,true,true,BUSY",
    "true,true,true,false,false,false,true,true,LINE_DEAD",
    "true,true,true,false,false,true,false,true,LINE_DEAD",
    "true,true,true,false,false,true,true,false,BUSY",
    "true,true,true,false,false,true,true,true,RINGING_OUT"
  })
  void dialingChecksEachRuleInOrder(
      boolean holding,
      boolean rate,
      boolean exists,
      boolean own,
      boolean busy,
      boolean answerable,
      boolean live,
      boolean lines,
      LineStatus expected) {
    assertEquals(
        expected,
        LineRules.dial(
            new LineRules.Dial(holding, rate, exists, own, busy, answerable, live, lines)));
  }
}
