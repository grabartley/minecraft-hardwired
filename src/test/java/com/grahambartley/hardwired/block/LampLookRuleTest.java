package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.LampLook;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LampLookRuleTest {

  private static final LampLook OWN = new LampLook(Hue.RED, 4);
  private static final LampLook NETWORK = new LampLook(Hue.BLUE, 9);

  private static Stream<Arguments> cases() {
    return Stream.of(
        Arguments.of(null, 0L, null, 0L, false),
        Arguments.of(null, 0L, NETWORK, 1L, false),
        Arguments.of(OWN, 1L, null, 0L, true),
        Arguments.of(OWN, 3L, NETWORK, 2L, true),
        Arguments.of(OWN, 2L, NETWORK, 3L, false));
  }

  @ParameterizedTest(name = "own {0} stamp {1}, network {2} stamp {3}: {4}")
  @MethodSource("cases")
  void theLatestChangeHolds(
      LampLook own, long ownStamp, LampLook network, long networkStamp, boolean stands) {
    assertEquals(stands, LampLookRule.ownLookStands(own, ownStamp, network, networkStamp));
  }
}
