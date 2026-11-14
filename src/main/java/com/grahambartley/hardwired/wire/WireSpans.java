package com.grahambartley.hardwired.wire;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireSpans {

  private final Set<WireSpan> spans = new LinkedHashSet<>();
  private final Long2ObjectOpenHashMap<LongList> columns = new Long2ObjectOpenHashMap<>();
  private final Long2LongOpenHashMap topOfBlock = new Long2LongOpenHashMap();

  public Set<WireSpan> all() {
    return Collections.unmodifiableSet(spans);
  }

  public List<WireSpan> at(BlockPos top) {
    return spans.stream().filter(span -> span.endsAt(top)).toList();
  }

  public boolean add(WireSpan span) {
    return spans.stream().noneMatch(span::sameRun) && spans.add(span);
  }

  public boolean restyle(WireSpan old, WireSpan next) {
    if (!old.sameRun(next) || !Objects.equals(old.owner(), next.owner()) || !spans.remove(old)) {
      return false;
    }
    spans.add(next);
    return true;
  }

  public void reown(Collection<WireCell> anchors, UUID owner) {
    for (WireCell anchor : anchors) {
      for (WireSpan span : List.copyOf(spans)) {
        if (span.color() == anchor.color()
            && Objects.equals(span.owner(), anchor.owner())
            && span.endsAt(anchor.face().pos())) {
          spans.remove(span);
          spans.add(span.ownedBy(owner));
        }
      }
    }
  }

  public boolean remove(WireSpan span) {
    return spans.remove(span);
  }

  public void setColumn(BlockPos top, LongList blocks) {
    clearColumn(top);
    columns.put(top.asLong(), blocks);
    for (long block : blocks) {
      topOfBlock.put(block, top.asLong());
    }
  }

  public void clearColumn(BlockPos top) {
    LongList blocks = columns.remove(top.asLong());
    if (blocks != null) {
      for (long block : blocks) {
        topOfBlock.remove(block);
      }
    }
  }

  public LongList column(BlockPos top) {
    return columns.get(top.asLong());
  }

  public Long2ObjectOpenHashMap<LongList> columns() {
    return columns;
  }

  public boolean isAnchor(WireCell cell) {
    return cell.face().side() == Direction.UP
        && spans.stream()
            .anyMatch(
                span ->
                    span.color() == cell.color()
                        && Objects.equals(span.owner(), cell.owner())
                        && span.endsAt(cell.face().pos()));
  }

  public List<WireCell> linkedTo(WireCell cell) {
    List<WireCell> linked = new ArrayList<>();
    if (!isAnchor(cell)) {
      return linked;
    }
    BlockPos top = cell.face().pos();
    for (WireSpan span : spans) {
      if (span.color() == cell.color()
          && Objects.equals(span.owner(), cell.owner())
          && span.endsAt(top)) {
        linked.add(WireSpan.anchor(span.otherEnd(top), span.color(), span.owner()));
      }
    }
    return linked;
  }

  public WireCell anchorFor(long block, WireCell touching) {
    if (!topOfBlock.containsKey(block)) {
      return null;
    }
    WireCell anchor =
        WireSpan.anchor(
            BlockPos.fromLong(topOfBlock.get(block)), touching.color(), touching.owner());
    return isAnchor(anchor) ? anchor : null;
  }
}
