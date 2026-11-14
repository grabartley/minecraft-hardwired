package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.wire.WireSpan;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.util.math.BlockPos;

public final class SpanMoves {

  public record Plan(List<WireSpan> moved, List<WireSpan> dropped) {}

  private SpanMoves() {}

  public static Plan plan(
      Collection<WireSpan> atTop,
      Collection<WireSpan> touchingLower,
      Set<BlockPos> lower,
      BlockPos top) {
    List<WireSpan> kept = new ArrayList<>(atTop);
    List<WireSpan> moved = new ArrayList<>();
    List<WireSpan> dropped = new ArrayList<>();
    for (WireSpan span : touchingLower) {
      BlockPos from = lower.contains(span.from()) ? top : span.from();
      BlockPos to = lower.contains(span.to()) ? top : span.to();
      WireSpan next =
          new WireSpan(from, to, span.color(), span.owner(), span.wire(), span.age(), span.waxed());
      boolean fits =
          !from.equals(to)
              && kept.stream().noneMatch(next::sameRun)
              && SpanRules.hasRoom((int) kept.stream().filter(s -> s.endsAt(top)).count());
      if (fits) {
        kept.add(next);
        moved.add(next);
      } else {
        dropped.add(span);
      }
    }
    return new Plan(List.copyOf(moved), List.copyOf(dropped));
  }
}
