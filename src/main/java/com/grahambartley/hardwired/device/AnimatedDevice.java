package com.grahambartley.hardwired.device;

import net.minecraft.block.BlockState;

public interface AnimatedDevice {

  boolean isAnimating(BlockState state);

  static boolean shownWhen(BlockState state, String rule) {
    return rule.startsWith("!") ? !flagged(state, rule.substring(1)) : flagged(state, rule);
  }

  static boolean flagged(BlockState state, String property) {
    return state.getProperties().stream()
        .anyMatch(
            candidate ->
                candidate.getName().equals(property) && Boolean.TRUE.equals(state.get(candidate)));
  }
}
