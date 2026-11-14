package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.block.BlockColumn;
import com.grahambartley.hardwired.block.TelephonePoleBlock;
import com.grahambartley.hardwired.registry.ModItems;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireSpan;
import com.grahambartley.hardwired.wire.WireViewer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class PoleLines {

  private final ServerWorld world;
  private final WireEngine engine;

  private PoleLines(ServerWorld world) {
    this.world = world;
    this.engine = WireEngine.of(world);
  }

  public static PoleLines of(ServerWorld world) {
    return new PoleLines(world);
  }

  public BlockPos topOf(BlockPos pos) {
    return pos.withY(columnAt(pos, false).top());
  }

  public void columnChanged(BlockPos pos) {
    BlockColumn column = columnAt(pos, false);
    BlockPos top = pos.withY(column.top());
    LongArrayList blocks = new LongArrayList();
    for (int y = column.bottom(); y <= column.top(); y++) {
      BlockPos block = pos.withY(y);
      blocks.add(block.asLong());
      if (y != column.top()) {
        engine.clearPoleColumn(block);
        moveSpans(block, top);
      }
    }
    engine.setPoleColumn(top, blocks);
  }

  public void poleBroken(BlockPos pos) {
    BlockColumn column = columnAt(pos, true);
    BlockPos top = pos.withY(column.top());
    for (WireSpan span : List.copyOf(engine.spansAt(top))) {
      engine.removeSpan(span);
      wireOf(span).forEach(stack -> Block.dropStack(world, pos, stack));
    }
    engine.clearPoleColumn(top);
    for (BlockPos rest : List.of(pos.down(), pos.up())) {
      if (TelephonePoleBlock.isPole(world.getBlockState(rest))) {
        columnChanged(rest);
      }
    }
  }

  public boolean string(WireSpan span) {
    return engine.addSpan(span);
  }

  public List<ItemStack> unstring(BlockPos top, Hue color) {
    List<ItemStack> wire = new ArrayList<>();
    for (WireSpan span : List.copyOf(engine.spansAt(top))) {
      if (span.color() == color && engine.removeSpan(span)) {
        wire.addAll(wireOf(span));
      }
    }
    return wire;
  }

  public boolean wax(BlockPos top, Predicate<WireSpan> allowed) {
    boolean waxed = false;
    for (WireSpan span : List.copyOf(engine.spansAt(top))) {
      if (span.canAge() && allowed.test(span)) {
        waxed |= engine.restyleSpan(span, span.withWax());
      }
    }
    return waxed;
  }

  public UUID ownerFor(BlockPos from, BlockPos to, Hue color, WireViewer viewer) {
    return Stream.concat(engine.spansAt(from).stream(), engine.spansAt(to).stream())
        .filter(span -> span.color() == color && span.owner() != null && viewer.canSee(span))
        .map(WireSpan::owner)
        .findFirst()
        .orElse(null);
  }

  public int spansAt(BlockPos top) {
    return engine.spansAt(top).size();
  }

  public boolean strung(BlockPos from, BlockPos to, Hue color) {
    WireSpan wanted = new WireSpan(from, to, color, null, 0);
    return engine.spansAt(from).stream().anyMatch(wanted::sameRun);
  }

  private static List<ItemStack> wireOf(WireSpan span) {
    List<ItemStack> stacks = new ArrayList<>();
    int left = span.wire();
    while (left > 0) {
      ItemStack stack = new ItemStack(ModItems.wire(span.color()));
      stack.setCount(Math.min(left, stack.getMaxCount()));
      left -= stack.getCount();
      stacks.add(stack);
    }
    return stacks;
  }

  private void moveSpans(BlockPos from, BlockPos to) {
    for (WireSpan span : List.copyOf(engine.spansAt(from))) {
      engine.removeSpan(span);
      engine.addSpan(span.movedEnd(from, to));
    }
  }

  private BlockColumn columnAt(BlockPos pos, boolean includingPos) {
    return BlockColumn.around(
        pos.getY(),
        world.getBottomY(),
        world.getTopY() - 1,
        y ->
            (includingPos && y == pos.getY())
                || TelephonePoleBlock.isPole(world.getBlockState(pos.withY(y))));
  }
}
