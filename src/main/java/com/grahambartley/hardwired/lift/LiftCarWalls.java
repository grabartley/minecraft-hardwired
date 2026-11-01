package com.grahambartley.hardwired.lift;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public final class LiftCarWalls {

  public static final double REACH_ABOVE_FLOOR = 2.0;

  private static final double WALL = 0.0625;
  private static final double CEILING = 1.85;
  private static final int WHOLE_TURN = 4;
  private static final double FLOOR_DEPTH = 0.125;

  private LiftCarWalls() {}

  public static Direction opening(int quarterTurns) {
    return Direction.fromHorizontal(((WHOLE_TURN - quarterTurns) % WHOLE_TURN + 2) % WHOLE_TURN);
  }

  public static List<Box> boxes(
      double x, double floorY, double z, int span, int quarterTurns, boolean sealed) {
    double half = span / 2.0;
    Box whole = new Box(x - half, floorY, z - half, x + half, floorY + REACH_ABOVE_FLOOR, z + half);
    Direction opening = opening(quarterTurns);
    List<Box> boxes = new ArrayList<>();
    boxes.add(whole.withMinY(floorY + CEILING));
    for (Direction side : Direction.Type.HORIZONTAL) {
      if (sealed || side != opening) {
        boxes.add(wall(whole.withMaxY(floorY + CEILING), side));
      }
    }
    return boxes;
  }

  public static Box floor(double x, double floorY, double z, int span, double feetY) {
    double half = span / 2.0;
    double top = feetY < floorY && feetY >= floorY - LiftSafety.REACH_BELOW_FLOOR ? feetY : floorY;
    return new Box(x - half, top - FLOOR_DEPTH, z - half, x + half, top, z + half);
  }

  private static Box wall(Box whole, Direction side) {
    return switch (side) {
      case NORTH -> whole.withMaxZ(whole.minZ + WALL);
      case SOUTH -> whole.withMinZ(whole.maxZ - WALL);
      case WEST -> whole.withMaxX(whole.minX + WALL);
      default -> whole.withMinX(whole.maxX - WALL);
    };
  }
}
