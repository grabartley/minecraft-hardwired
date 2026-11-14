package com.grahambartley.hardwired.device;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

public interface AnimatedDevice {

  boolean isAnimating(BlockState state);

  static boolean shownWhen(BlockState state, String rule) {
    if (rule.startsWith("!")) {
      return !flagged(state, rule.substring(1));
    }
    int split = rule.indexOf('=');
    return split < 0
        ? flagged(state, rule)
        : holds(state, rule.substring(0, split), rule.substring(split + 1));
  }

  static boolean holds(BlockState state, String property, String value) {
    return state.getProperties().stream()
        .anyMatch(
            candidate ->
                candidate.getName().equals(property) && valueName(state, candidate).equals(value));
  }

  private static <T extends Comparable<T>> String valueName(
      BlockState state, Property<T> property) {
    return property.name(state.get(property));
  }

  static boolean flagged(BlockState state, String property) {
    return state.getProperties().stream()
        .anyMatch(
            candidate ->
                candidate.getName().equals(property) && Boolean.TRUE.equals(state.get(candidate)));
  }
}
