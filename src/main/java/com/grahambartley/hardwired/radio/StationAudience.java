package com.grahambartley.hardwired.radio;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class StationAudience<T> {

  private final Map<BlockPos, Set<T>> tuned = new HashMap<>();

  public void retune(T device, @Nullable BlockPos from, @Nullable BlockPos to) {
    if (Objects.equals(from, to)) {
      return;
    }
    if (from != null) {
      leave(device, from);
    }
    if (to != null) {
      tuned.computeIfAbsent(to.toImmutable(), desk -> new HashSet<>()).add(device);
    }
  }

  public Set<T> tunedTo(BlockPos desk) {
    return Collections.unmodifiableSet(tuned.getOrDefault(desk, Set.of()));
  }

  private void leave(T device, BlockPos desk) {
    Set<T> devices = tuned.get(desk);
    if (devices != null && devices.remove(device) && devices.isEmpty()) {
      tuned.remove(desk);
    }
  }
}
