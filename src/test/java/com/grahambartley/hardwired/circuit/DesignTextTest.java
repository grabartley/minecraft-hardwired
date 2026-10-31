package com.grahambartley.hardwired.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.circuit.DesignText.Result;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class DesignTextTest {

  private static final int LIMIT = 64;
  private static final String GOOD = "HWC1|ioui|and:1:2:0|0:4:0,3:4:1,4:1:0";

  private static Stream<Design> designs() {
    return Stream.of(
        Design.EMPTY,
        Designs.twoInputPart(PartType.COUNTER, 16),
        Designs.oneInputPart(PartType.TIMER, 1200),
        Designs.notThroughTimerOscillator(3));
  }

  @ParameterizedTest(name = "design {index} round trips")
  @MethodSource("designs")
  void designsRoundTrip(Design design) {
    assertEquals(new Result.Accepted(design), DesignText.parse(DesignText.encode(design), LIMIT));
  }

  @ParameterizedTest(name = "a {0} round trips")
  @EnumSource(PartType.class)
  void everyPartTypeRoundTrips(PartType type) {
    Design design =
        type.inputs() == 2
            ? Designs.twoInputPart(type, type.defaultSetting())
            : Designs.oneInputPart(type, type.defaultSetting());
    assertEquals(new Result.Accepted(design), DesignText.parse(DesignText.encode(design), LIMIT));
  }

  @Test
  void theFormatStaysReadable() {
    assertEquals(GOOD, DesignText.encode(Designs.twoInputPartAt(1, 2)));
  }

  @Test
  void surroundingSpaceFromAPasteIsIgnored() {
    assertInstanceOf(Result.Accepted.class, DesignText.parse("  " + GOOD + "\n", LIMIT));
  }

  @Test
  void theFullestLegalDesignFitsTheLimit() {
    java.util.List<Part> parts = new java.util.ArrayList<>();
    java.util.List<Line> lines = new java.util.ArrayList<>();
    for (int index = 0; index < 256; index++) {
      parts.add(
          new Part(PartType.COUNTER, index % Design.GRID_WIDTH, index / Design.GRID_WIDTH, 256));
      lines.add(new Line(Design.partNode((index + 1) % 256), Design.partNode(index), 0));
      lines.add(new Line(Design.partNode((index + 1) % 256), Design.partNode(index), 1));
    }
    String text = DesignText.encode(new Design(Designs.TWO_IN_ONE_OUT, parts, lines));
    assertTrue(text.length() < DesignText.MAX_LENGTH, "length " + text.length());
  }

  private static Stream<Arguments> hostile() {
    return Stream.of(
        Arguments.of("nothing", "", Refusal.BAD_TEXT),
        Arguments.of("spaces", "    ", Refusal.BAD_TEXT),
        Arguments.of("chat", "hello world", Refusal.BAD_TEXT),
        Arguments.of("only a prefix", "HWC1", Refusal.BAD_TEXT),
        Arguments.of("a later version", "HWC2|uuuu||", Refusal.NEWER_VERSION),
        Arguments.of("too few sections", "HWC1|uuuu|", Refusal.BAD_TEXT),
        Arguments.of("too many sections", "HWC1|uuuu|||", Refusal.BAD_TEXT),
        Arguments.of("too few sides", "HWC1|uuu||", Refusal.BAD_TEXT),
        Arguments.of("too many sides", "HWC1|uuuuu||", Refusal.BAD_TEXT),
        Arguments.of("an unknown side", "HWC1|uuxu||", Refusal.BAD_TEXT),
        Arguments.of("an unknown part", "HWC1|uuuu|flipflop:0:0:0|", Refusal.BAD_TEXT),
        Arguments.of("a part with no fields", "HWC1|uuuu|and|", Refusal.BAD_TEXT),
        Arguments.of("a part with an extra field", "HWC1|uuuu|and:1:1:0:9|", Refusal.BAD_TEXT),
        Arguments.of("a blank part", "HWC1|uuuu|and:1:1:0,|", Refusal.BAD_TEXT),
        Arguments.of("a negative x", "HWC1|uuuu|and:-1:1:0|", Refusal.BAD_TEXT),
        Arguments.of("a number too big", "HWC1|uuuu|and:99999999999999:1:0|", Refusal.BAD_TEXT),
        Arguments.of("a word for a number", "HWC1|uuuu|and:one:1:0|", Refusal.BAD_TEXT),
        Arguments.of("a blank line", "HWC1|iuuu|not:1:1:0|0:4", Refusal.BAD_TEXT),
        Arguments.of("off the grid", "HWC1|uuuu|and:20:1:0|", Refusal.OFF_THE_GRID),
        Arguments.of("overlapping", "HWC1|uuuu|and:1:1:0,or:1:1:0|", Refusal.PARTS_OVERLAP),
        Arguments.of("a bad setting", "HWC1|uuuu|timer:1:1:0|", Refusal.BAD_SETTING),
        Arguments.of("a bad line", "HWC1|uuuu|not:1:1:0|0:4:0", Refusal.BAD_LINE),
        Arguments.of("a taken pin", "HWC1|iuui|not:1:1:0|0:4:0,3:4:0", Refusal.INPUT_TAKEN),
        Arguments.of("a loop", "HWC1|uuuu|not:1:1:0|4:4:0", Refusal.LOOP_WITHOUT_DELAY),
        Arguments.of("too long", "x".repeat(DesignText.MAX_LENGTH + 1), Refusal.TOO_LONG),
        Arguments.of(
            "too many parts for the read",
            "HWC1|uuuu|" + "and:1:1:0,".repeat(2000) + "and:1:1:0|",
            Refusal.BAD_TEXT),
        Arguments.of(
            "too many lines for the read",
            "HWC1|iuuu|not:1:1:0|" + "0:4:0,".repeat(5000) + "0:4:0",
            Refusal.BAD_TEXT));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("hostile")
  void hostileTextIsRefusedWithAReason(String name, String text, Refusal reason) {
    assertEquals(new Result.Refused(reason), DesignText.parse(text, LIMIT));
  }

  @Test
  void thePartLimitAppliesToPastes() {
    StringBuilder text = new StringBuilder("HWC1|uuuu|and:0:0:0");
    for (int x = 1; x < 9; x++) {
      text.append(",and:").append(x).append(":0:0");
    }
    text.append('|');
    assertEquals(new Result.Refused(Refusal.TOO_MANY_PARTS), DesignText.parse(text.toString(), 8));
  }
}
