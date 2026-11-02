package com.grahambartley.hardwired.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlowTest {

  private static final BlockPos POS = new BlockPos(1, 2, 3);
  private static final int RGB = 0x123456;

  @ParameterizedTest(name = "level {0} glows: {1}")
  @CsvSource({"15,true", "1,true", "0,false"})
  void glowsOnlyWhenLit(int level, boolean glows) {
    Optional<Glow> expected = glows ? Optional.of(new Glow(POS, level, RGB)) : Optional.empty();
    assertEquals(expected, Glow.of(POS, level, RGB));
  }
}
