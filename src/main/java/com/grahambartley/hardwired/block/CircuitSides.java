package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.circuit.Side;
import net.minecraft.util.math.Direction;

public final class CircuitSides {

  private CircuitSides() {}

  public static Direction direction(Side side) {
    return switch (side) {
      case NORTH -> Direction.NORTH;
      case EAST -> Direction.EAST;
      case SOUTH -> Direction.SOUTH;
      case WEST -> Direction.WEST;
    };
  }

  public static Side side(Direction direction) {
    return switch (direction) {
      case NORTH -> Side.NORTH;
      case EAST -> Side.EAST;
      case SOUTH -> Side.SOUTH;
      case WEST -> Side.WEST;
      default -> null;
    };
  }

  public static int turnsFor(Direction facing) {
    Side side = side(facing);
    return side == null ? 0 : side.ordinal();
  }
}
