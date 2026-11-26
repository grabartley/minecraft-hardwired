package com.grahambartley.hardwired.config;

import java.util.Optional;
import net.minecraft.text.Text;

public final class ConfigRefusals {
  static final String TOWER_MASTS = "config.hardwired.refused.towermasts";

  private ConfigRefusals() {}

  public static Optional<Text> of(final HardwiredConfig config) {
    final RadioSettings radio = config.radio();
    if (!radio.towerMastsInOrder()) {
      return Optional.of(
          Text.translatable(TOWER_MASTS, radio.towerMinMasts(), radio.towerMaxMasts()));
    }
    return Optional.empty();
  }
}
