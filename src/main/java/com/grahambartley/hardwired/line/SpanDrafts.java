package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.wire.Hue;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class SpanDrafts {

  public record Draft(Identifier world, BlockPos start, Hue color) {}

  private final Map<UUID, Draft> drafts = new HashMap<>();

  public Optional<Draft> of(UUID player, Identifier world) {
    return Optional.ofNullable(drafts.get(player)).filter(draft -> draft.world().equals(world));
  }

  public void start(UUID player, Draft draft) {
    drafts.put(player, draft);
  }

  public void end(UUID player) {
    drafts.remove(player);
  }

  public void dropWhere(BiPredicate<UUID, Draft> stale) {
    drafts.entrySet().removeIf(entry -> stale.test(entry.getKey(), entry.getValue()));
  }

  public void clear() {
    drafts.clear();
  }
}
