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
import java.util.function.BiConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class WireSpans {

  private final Set<WireSpan> spans = new LinkedHashSet<>();
  private final Long2ObjectOpenHashMap<List<WireSpan>> byTop = new Long2ObjectOpenHashMap<>();
  private final Long2ObjectOpenHashMap<LongList> columns = new Long2ObjectOpenHashMap<>();
  private final Long2LongOpenHashMap topOfBlock = new Long2LongOpenHashMap();

  public Set<WireSpan> all() {
    return Collections.unmodifiableSet(spans);
  }

  public List<WireSpan> at(BlockPos top) {
    List<WireSpan> found = byTop.get(top.asLong());
    return found == null ? List.of() : List.copyOf(found);
  }

  public boolean add(WireSpan span) {
    if (at(span.from()).stream().anyMatch(span::sameRun) || !spans.add(span)) {
      return false;
    }
    index(span);
    return true;
  }

  public boolean remove(WireSpan span) {
    if (!spans.remove(span)) {
      return false;
    }
    unindex(span);
    return true;
  }

  public boolean restyle(WireSpan old, WireSpan next) {
    return old.sameRun(next) && Objects.equals(old.owner(), next.owner()) && replace(old, next);
  }

  public void reown(Collection<WireCell> anchors, UUID owner) {
    for (WireCell anchor : anchors) {
      for (WireSpan span : at(anchor.face().pos())) {
        if (anchors(span, anchor)) {
          replace(span, span.ownedBy(owner));
        }
      }
    }
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

  public void forEachColumn(BiConsumer<BlockPos, LongList> action) {
    columns.forEach((top, blocks) -> action.accept(BlockPos.fromLong(top), blocks));
  }

  public boolean isAnchor(WireCell cell) {
    return cell.face().side() == Direction.UP
        && at(cell.face().pos()).stream().anyMatch(span -> anchors(span, cell));
  }

  public List<WireCell> linkedTo(WireCell cell) {
    if (cell.face().side() != Direction.UP) {
      return List.of();
    }
    BlockPos top = cell.face().pos();
    return at(top).stream()
        .filter(span -> anchors(span, cell))
        .map(span -> WireSpan.anchor(span.otherEnd(top), span.color(), span.owner()))
        .toList();
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

  private boolean replace(WireSpan old, WireSpan next) {
    if (!remove(old)) {
      return false;
    }
    spans.add(next);
    index(next);
    return true;
  }

  private static boolean anchors(WireSpan span, WireCell cell) {
    return span.color() == cell.color() && Objects.equals(span.owner(), cell.owner());
  }

  private void index(WireSpan span) {
    for (BlockPos top : List.of(span.from(), span.to())) {
      byTop.computeIfAbsent(top.asLong(), key -> new ArrayList<>()).add(span);
    }
  }

  private void unindex(WireSpan span) {
    for (BlockPos top : List.of(span.from(), span.to())) {
      List<WireSpan> found = byTop.get(top.asLong());
      if (found != null) {
        found.remove(span);
        if (found.isEmpty()) {
          byTop.remove(top.asLong());
        }
      }
    }
  }
}
