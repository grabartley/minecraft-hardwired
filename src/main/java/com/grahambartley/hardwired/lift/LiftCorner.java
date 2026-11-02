package com.grahambartley.hardwired.lift;

import java.util.List;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public enum LiftCorner implements StringIdentifiable {
  NORTH_WEST("north_west", 0, 0),
  NORTH_EAST("north_east", 1, 0),
  SOUTH_WEST("south_west", 0, 1),
  SOUTH_EAST("south_east", 1, 1);

  private static final int MIDDLE_TWICE = 1;

  private final String id;
  private final int east;
  private final int south;

  LiftCorner(String id, int east, int south) {
    this.id = id;
    this.east = east;
    this.south = south;
  }

  @Override
  public String asString() {
    return id;
  }

  public BlockPos head(BlockPos cell) {
    return cell.add(-east, 0, -south);
  }

  public BlockPos cell(BlockPos head) {
    return head.add(east, 0, south);
  }

  public Direction facing(Direction front) {
    int reach =
        (east * 2 - MIDDLE_TWICE) * front.getOffsetX()
            + (south * 2 - MIDDLE_TWICE) * front.getOffsetZ();
    return reach > 0 ? front : front.getOpposite();
  }

  public static List<BlockPos> cells(BlockPos head) {
    return List.of(
        NORTH_WEST.cell(head), NORTH_EAST.cell(head), SOUTH_WEST.cell(head), SOUTH_EAST.cell(head));
  }

  public static LiftCorner of(BlockPos head, BlockPos cell) {
    for (LiftCorner corner : values()) {
      if (corner.cell(head).equals(cell)) {
        return corner;
      }
    }
    return NORTH_WEST;
  }

  public static BlockPos headFor(BlockPos placed, Direction look) {
    BlockPos far = placed.offset(look).offset(look.rotateYClockwise());
    return new BlockPos(
        Math.min(placed.getX(), far.getX()), placed.getY(), Math.min(placed.getZ(), far.getZ()));
  }
}
