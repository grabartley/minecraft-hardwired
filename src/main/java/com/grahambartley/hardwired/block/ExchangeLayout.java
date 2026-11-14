package com.grahambartley.hardwired.block;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ExchangeLayout {

  private ExchangeLayout() {}

  public static Direction rightOf(Direction facing) {
    return facing.rotateYCounterclockwise();
  }

  public static BlockPos cell(ExchangePart part, Direction facing, BlockPos anchor) {
    BlockPos cell = part.right() ? anchor.offset(rightOf(facing)) : anchor;
    return part.upper() ? cell.up() : cell;
  }

  public static BlockPos anchorOf(ExchangePart part, Direction facing, BlockPos pos) {
    BlockPos anchor = part.right() ? pos.offset(rightOf(facing).getOpposite()) : pos;
    return part.upper() ? anchor.down() : anchor;
  }

  public static List<BlockPos> cells(Direction facing, BlockPos anchor) {
    return Arrays.stream(ExchangePart.values()).map(part -> cell(part, facing, anchor)).toList();
  }
}
