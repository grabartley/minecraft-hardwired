package com.grahambartley.hardwired.radio;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.RadioMastBlock;
import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class TowerFinder {

  public record Found(BlockPos foot, TowerColumn column) {

    public int reach() {
      return column.reach(Hardwired.SERVER_CONFIG.radio());
    }
  }

  private TowerFinder() {}

  public static Optional<Found> tallest(ServerWorld world, BlockPos wired) {
    RadioSettings settings = Hardwired.SERVER_CONFIG.radio();
    return WireEngine.of(world).loadedOnNetwork(wired).stream()
        .filter(pos -> world.getBlockState(pos).getBlock() instanceof RadioMastBlock)
        .map(pos -> at(world, pos))
        .max(
            Comparator.<Found, Boolean>comparing(found -> found.column().stands(settings))
                .thenComparingInt(found -> found.column().masts()));
  }

  private static Found at(ServerWorld world, BlockPos pos) {
    TowerColumn column =
        TowerColumn.read(
            pos.getY(),
            world.getBottomY(),
            world.getTopY() - 1,
            y -> cell(world.getBlockState(pos.withY(y))));
    return new Found(pos.withY(column.footY()), column);
  }

  private static TowerColumn.Cell cell(BlockState state) {
    if (state.getBlock() instanceof RadioMastBlock) {
      return TowerColumn.Cell.MAST;
    }
    return RadioMastBlock.isRod(state) ? TowerColumn.Cell.ROD : TowerColumn.Cell.OTHER;
  }
}
