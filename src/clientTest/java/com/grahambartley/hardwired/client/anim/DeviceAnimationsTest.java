package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.stream.Stream;
import net.minecraft.client.render.LightmapTextureManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class DeviceAnimationsTest {

  private static final int WORLD_LIGHT = 123;

  private static JsonObject part(String extra) {
    return JsonParser.parseString(
            "{\"bone\":\"b\",\"model\":\"hardwired:block/part/b\",\"pivot\":[1,2,3]" + extra + "}")
        .getAsJsonObject();
  }

  static Stream<Arguments> parts() {
    return Stream.of(
        Arguments.of("", false),
        Arguments.of(",\"glow\":false", false),
        Arguments.of(",\"glow\":true", true));
  }

  @ParameterizedTest
  @MethodSource("parts")
  void aPartGlowsOnlyWhenItsDataSaysSo(String extra, boolean glow) {
    assertEquals(glow, DeviceAnimations.part(part(extra)).glow());
  }

  @ParameterizedTest
  @CsvSource({"false,false,true", "false,true,true", "true,false,false", "true,true,true"})
  void aGlowPartIsDrawnOnlyWhileRunning(boolean glow, boolean animating, boolean drawn) {
    String extra = glow ? ",\"glow\":true" : "";
    assertEquals(drawn, DeviceAnimations.part(part(extra)).drawnWhile(animating));
  }

  @ParameterizedTest
  @MethodSource("conditions")
  void aPartReadsItsConditionFromData(String extra, String when) {
    assertEquals(when, DeviceAnimations.part(part(extra)).when());
  }

  static Stream<Arguments> conditions() {
    return Stream.of(Arguments.of("", null), Arguments.of(",\"when\":\"disc\"", "disc"));
  }

  @ParameterizedTest
  @MethodSource("motions")
  void aPartReadsWhenItMovesFromData(String extra, String movesWhen) {
    assertEquals(movesWhen, DeviceAnimations.part(part(extra)).movesWhen());
  }

  static Stream<Arguments> motions() {
    return Stream.of(Arguments.of("", null), Arguments.of(",\"moves_when\":\"disc\"", "disc"));
  }

  @ParameterizedTest
  @MethodSource("parts")
  void aPartWithNoMotionRuleAlwaysMoves(String extra, boolean glow) {
    assertTrue(DeviceAnimations.part(part(extra)).movesIn(null));
  }

  @ParameterizedTest
  @MethodSource("parts")
  void aGlowPartIgnoresWorldLight(String extra, boolean glow) {
    int expected = glow ? LightmapTextureManager.MAX_LIGHT_COORDINATE : WORLD_LIGHT;
    assertEquals(expected, DeviceAnimations.part(part(extra)).light(WORLD_LIGHT));
  }
}
