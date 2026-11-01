package com.grahambartley.hardwired.light;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;

public record Glow(BlockPos pos, int level, int rgb) {

  public static final int MAX_LEVEL = 15;

  public static Optional<Glow> of(BlockPos pos, int level, Hue hue) {
    return hue.castsColour() ? of(pos, level, hue.lampRgb()) : Optional.empty();
  }

  public static Optional<Glow> of(BlockPos pos, int level, int rgb) {
    return level > 0 ? Optional.of(new Glow(pos, level, rgb)) : Optional.empty();
  }
}
