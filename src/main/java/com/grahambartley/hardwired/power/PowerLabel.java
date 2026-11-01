package com.grahambartley.hardwired.power;

import com.grahambartley.hardwired.block.LiftBlock;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.device.WireDevice;
import java.util.Optional;
import net.minecraft.block.Block;

public record PowerLabel(String key, int amount) {

  private static final String GIVES = "tooltip.hardwired.gives_power";
  private static final String DRAWS = "tooltip.hardwired.draws_power";
  private static final String DRAWS_MOVING = "tooltip.hardwired.draws_power_moving";

  public static Optional<PowerLabel> of(Block block, PowerSettings settings) {
    if (block instanceof PowerSource source) {
      return Optional.of(new PowerLabel(GIVES, source.sourceKind().rating(settings)));
    }
    if (block instanceof WireDevice device && device.ratedLoad(settings) > 0) {
      String key = block instanceof LiftBlock ? DRAWS_MOVING : DRAWS;
      return Optional.of(new PowerLabel(key, device.ratedLoad(settings)));
    }
    return Optional.empty();
  }
}
