package com.grahambartley.hardwired.client.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpanColoursTest {

  @ParameterizedTest(name = "{0} age {1} on {2}")
  @CsvSource({
    "PLAIN,0,false,C46D51",
    "PLAIN,1,false,A1806A",
    "PLAIN,2,false,6C9A6E",
    "PLAIN,3,false,52A284",
    "PLAIN,9,false,52A284",
    "PLAIN,0,true,D9A08E",
    "BLUE,3,false,-"
  })
  void plainCopperShowsItsAgeAndALiveLineShowsBrighter(
      Hue hue, int age, boolean on, String expected) {
    int want = expected.equals("-") ? hue.rgb() : Integer.parseInt(expected, 16);
    assertEquals(want, SpanColours.rgb(hue.ordinal(), age, on));
  }
}
