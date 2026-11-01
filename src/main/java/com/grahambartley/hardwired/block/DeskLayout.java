package com.grahambartley.hardwired.block;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class DeskLayout {

  private DeskLayout() {}

  public static Direction facingFor(Direction playerFacing) {
    return playerFacing.rotateYCounterclockwise();
  }

  public static BlockPos panelOf(DeskPart part, Direction facing, BlockPos pos) {
    return switch (part) {
      case PANEL -> pos;
      case TOP -> pos.down();
      case DESK -> pos.offset(facing);
    };
  }

  public static BlockPos cell(DeskPart part, Direction facing, BlockPos panel) {
    return switch (part) {
      case PANEL -> panel;
      case TOP -> panel.up();
      case DESK -> panel.offset(facing.getOpposite());
    };
  }

  public static List<BlockPos> cells(Direction facing, BlockPos panel) {
    return List.of(
        cell(DeskPart.PANEL, facing, panel),
        cell(DeskPart.TOP, facing, panel),
        cell(DeskPart.DESK, facing, panel));
  }
}
