package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.util.DyeColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HueTest {

  @ParameterizedTest(name = "{0} has its own hue")
  @EnumSource(DyeColor.class)
  void everyDyeMapsToItsOwnHue(DyeColor dye) {
    assertEquals(dye, Hue.ofDye(dye).dye().orElseThrow());
  }

  @Test
  void plainLampsAreWarmWhiteNotCopper() {
    assertNotEquals(Hue.PLAIN.rgb(), Hue.PLAIN.lampRgb());
  }

  @Test
  void indexesWrap() {
    assertEquals(Hue.PLAIN, Hue.byIndex(Hue.values().length));
  }
}
