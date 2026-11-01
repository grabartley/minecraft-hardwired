package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowColorTest {

  private static final float EPSILON = 0.001F;

  @ParameterizedTest(name = "{0} is {1},{2},{3}")
  @CsvSource({
    "0xFF0000,1,0,0",
    "0x800000,0.502,0,0",
    "0x804000,0.502,0.251,0",
    "0x000000,0,0,0",
    "0xFFFFFF,1,1,1"
  })
  void keepsTheDarknessOfTheColour(String rgb, float red, float green, float blue) {
    assertArrayEquals(
        new float[] {red, green, blue}, GlowColor.channels(Integer.decode(rgb)), 0.01F);
  }

  @ParameterizedTest(name = "{0} stays darker than {1}")
  @CsvSource({"BLACK,WHITE", "GRAY,LIGHT_GRAY", "LIGHT_GRAY,WHITE"})
  void darkDyesCastLessThanLightOnes(Hue dark, Hue light) {
    float darkTop = GlowColor.channels(dark.lampRgb())[2];
    float lightTop = GlowColor.channels(light.lampRgb())[2];
    assertTrue(darkTop < lightTop);
  }

  @ParameterizedTest(name = "sky {0}, darkness {1} gives {2}")
  @CsvSource({"15,0,1", "0,0,0", "15,11,0", "15,4,0.636", "7,0,0.467"})
  void daylightFollowsSkyLightAndTimeOfDay(int sky, int darkness, float expected) {
    assertEquals(expected, GlowColor.daylight(sky, darkness), EPSILON);
  }

  @ParameterizedTest(name = "weight {0} in daylight {1} gives {2}")
  @CsvSource({"0,0,0", "1,0,1", "0.5,0,1", "1,0.5,0.5", "1,1,0", "0,1,0"})
  void strengthFadesToNothingAndWithDaylight(float weight, float daylight, float expected) {
    assertEquals(expected, GlowColor.strength(weight, daylight), EPSILON);
  }

  @ParameterizedTest(name = "hue {0} at strength {1}, removing {2}, gives {3}")
  @CsvSource({
    "1,0.5,false,0.5",
    "0,0.5,false,0",
    "1,0.5,true,0",
    "0,0.5,true,0.5",
    "0,0,true,0",
    "1,0,false,0"
  })
  void nothingIsChangedAtZeroStrength(float hue, float strength, boolean removing, float out) {
    assertEquals(out, GlowColor.blend(hue, strength, removing), EPSILON);
  }

  @ParameterizedTest(name = "falls off gently from weight {0}")
  @CsvSource({"0.01", "0.03", "0.06"})
  void edgeOfTheLightIsSofterThanALine(float weight) {
    assertTrue(GlowColor.strength(weight, 0F) < weight * 2.2F);
  }
}
