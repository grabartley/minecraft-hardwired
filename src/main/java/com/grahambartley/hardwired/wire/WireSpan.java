package com.grahambartley.hardwired.wire;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record WireSpan(
    BlockPos from, BlockPos to, Hue color, UUID owner, int wire, int age, boolean waxed) {

  public static final int MAX_AGE = 3;

  public WireSpan(BlockPos from, BlockPos to, Hue color, UUID owner, int wire) {
    this(from, to, color, owner, wire, 0, false);
  }

  public WireSpan {
    age = Math.clamp(age, 0, MAX_AGE);
    from = from.toImmutable();
    to = to.toImmutable();
    if (from.asLong() > to.asLong()) {
      BlockPos swap = from;
      from = to;
      to = swap;
    }
  }

  public static WireCell anchor(BlockPos top, Hue color, UUID owner) {
    return new WireCell(new WireFace(top.toImmutable(), Direction.UP), color, owner);
  }

  public WireCell fromAnchor() {
    return anchor(from, color, owner);
  }

  public WireCell toAnchor() {
    return anchor(to, color, owner);
  }

  public boolean endsAt(BlockPos top) {
    return from.equals(top) || to.equals(top);
  }

  public BlockPos otherEnd(BlockPos top) {
    return from.equals(top) ? to : from;
  }

  public boolean canAge() {
    return color == Hue.PLAIN && !waxed && age < MAX_AGE;
  }

  public WireSpan older() {
    return new WireSpan(from, to, color, owner, wire, age + 1, waxed);
  }

  public WireSpan withWax() {
    return new WireSpan(from, to, color, owner, wire, age, true);
  }

  public boolean sameRun(WireSpan other) {
    return from.equals(other.from) && to.equals(other.to) && color == other.color;
  }

  public double length() {
    return Math.sqrt(from.getSquaredDistance(to));
  }

  public WireSpan ownedBy(UUID newOwner) {
    return new WireSpan(from, to, color, newOwner, wire, age, waxed);
  }

  public WireSpan movedEnd(BlockPos oldTop, BlockPos newTop) {
    return from.equals(oldTop)
        ? new WireSpan(newTop, to, color, owner, wire, age, waxed)
        : new WireSpan(from, newTop, color, owner, wire, age, waxed);
  }
}
