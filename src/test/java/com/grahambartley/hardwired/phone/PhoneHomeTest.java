package com.grahambartley.hardwired.phone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneHomeTest {

  private static Set<Hue> hues(String list) {
    return list.isEmpty()
        ? Set.of()
        : Arrays.stream(list.split(" ")).map(Hue::valueOf).collect(Collectors.toSet());
  }

  private static Optional<Hue> hue(String name) {
    return name.isEmpty() ? Optional.empty() : Optional.of(Hue.valueOf(name));
  }

  @ParameterizedTest(name = "home {0} partners [{1}] gives {2}")
  @CsvSource({
    "'','',''",
    "'','RED',RED",
    "'','RED WHITE',WHITE",
    "RED,'RED WHITE',RED",
    "RED,'WHITE BLUE',WHITE",
    "RED,'',''",
    "BLUE,'PLAIN BLUE',BLUE"
  })
  void aDeviceStaysOnItsFirstNetworkWhileThatStillHoldsAPartner(
      String current, String partners, String expected) {
    assertEquals(hue(expected), PhoneHome.choose(hue(current), hues(partners)));
  }
}
