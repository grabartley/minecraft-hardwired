package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.WireWax.Action;
import com.grahambartley.hardwired.wire.WireWax.Held;
import com.grahambartley.hardwired.wire.WireWax.Use;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.Hand;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WireWaxTest {

  private static final Held WIRE = new Held(Hue.RED, null);
  private static final Held HONEYCOMB = new Held(null, Action.SEAL);
  private static final Held AXE = new Held(null, Action.UNSEAL);
  private static final Held OTHER = new Held(null, null);

  private static Stream<Arguments> uses() {
    return Stream.of(
        Arguments.of(WIRE, HONEYCOMB, false, Optional.of(use(Action.SEAL, Hand.OFF_HAND))),
        Arguments.of(HONEYCOMB, WIRE, false, Optional.of(use(Action.SEAL, Hand.MAIN_HAND))),
        Arguments.of(WIRE, AXE, false, Optional.of(use(Action.UNSEAL, Hand.OFF_HAND))),
        Arguments.of(AXE, WIRE, false, Optional.of(use(Action.UNSEAL, Hand.MAIN_HAND))),
        Arguments.of(HONEYCOMB, WIRE, true, Optional.empty()),
        Arguments.of(HONEYCOMB, OTHER, false, Optional.empty()),
        Arguments.of(WIRE, OTHER, false, Optional.empty()),
        Arguments.of(WIRE, WIRE, false, Optional.empty()),
        Arguments.of(HONEYCOMB, AXE, false, Optional.empty()));
  }

  private static Use use(Action action, Hand hand) {
    return new Use(action, hand, Hue.RED);
  }

  @ParameterizedTest(name = "{0} and {1}, sneaking {2}")
  @MethodSource("uses")
  void theToolNeedsWireInTheOtherHand(
      Held main, Held off, boolean sneaking, Optional<Use> expected) {
    assertEquals(expected, WireWax.decide(main, off, sneaking));
  }
}
