package com.grahambartley.hardwired.client.glow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowColorTest {

  private static final float EPSILON = 0.001F;

  @ParameterizedTest(name = "{0} saturates to {1},{2},{3}")
  @CsvSource({
    "0xFF0000,1,0,0",
    "0x800000,1,0,0",
    "0x804000,1,0.5,0",
    "0x000080,0,0,1",
    "0xFFFFFF,1,1,1"
  })
  void brightestChannelBecomesFull(String rgb, float red, float green, float blue) {
    float[] hue = GlowColor.saturated(Integer.decode(rgb));
    assertArrayEquals(new float[] {red, green, blue}, hue, 0.01F);
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
