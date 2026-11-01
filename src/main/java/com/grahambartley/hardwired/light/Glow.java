package com.grahambartley.hardwired.light;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;

public record Glow(BlockPos pos, int level, int rgb) {

  public static Optional<Glow> of(BlockPos pos, int level, Hue hue) {
    return level > 0 && hue.castsColour()
        ? Optional.of(new Glow(pos, level, hue.lampRgb()))
        : Optional.empty();
  }
}
