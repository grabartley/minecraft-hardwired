package com.grahambartley.hardwired.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowTest {

  private static final BlockPos POS = new BlockPos(1, 2, 3);

  @ParameterizedTest(name = "{0} at level {1} glows: {2}")
  @CsvSource({"RED,15,true", "RED,1,true", "RED,0,false", "PLAIN,15,false", "PLAIN,0,false"})
  void glowsOnlyWhenLitAndColoured(Hue hue, int level, boolean glows) {
    assertEquals(glows, Glow.of(POS, level, hue).isPresent());
  }

  @ParameterizedTest(name = "{0} glows {1}")
  @CsvSource({"RED,11546150", "BLUE,3949738"})
  void glowCarriesPositionLevelAndColour(Hue hue, int rgb) {
    assertEquals(Optional.of(new Glow(POS, 9, rgb)), Glow.of(POS, 9, hue));
  }

  @ParameterizedTest(name = "fixed colour at level {0} glows: {1}")
  @CsvSource({"15,true", "1,true", "0,false"})
  void anyColourFollowsTheSameLevelRule(int level, boolean glows) {
    assertEquals(glows, Glow.of(POS, level, 0x123456).isPresent());
  }
}
