package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.config.PowerSettings;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireEngine;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface WireDevice {

  static List<BlockPos> twoTall(BlockPos pos, DoubleBlockHalf half) {
    BlockPos lower = half == DoubleBlockHalf.UPPER ? pos.down() : pos;
    return List.of(lower, lower.up());
  }

  static boolean fits(ItemPlacementContext context, BlockState state, List<BlockPos> cells) {
    World world = context.getWorld();
    return cells.stream()
        .allMatch(
            cell ->
                world.isInBuildLimit(cell)
                    && world.getWorldBorder().contains(cell)
                    && world.getBlockState(cell).canReplace(context)
                    && world.canPlace(state, cell, ShapeContext.absent()));
  }

  static void clearOthers(World world, BlockPos pos, Block block, List<BlockPos> cells) {
    for (BlockPos cell : cells) {
      if (!cell.equals(pos) && world.getBlockState(cell).isOf(block)) {
        world.setBlockState(
            cell, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
      }
    }
  }

  default boolean isDriving(ServerWorld world, BlockPos pos, BlockState state, Hue color) {
    return false;
  }

  default boolean connectsFrom(BlockState state, Direction side) {
    return true;
  }

  default boolean needsWireOn() {
    return false;
  }

  default boolean showsGridTotals() {
    return false;
  }

  default boolean isSwitch() {
    return false;
  }

  default boolean hasPorts() {
    return false;
  }

  default boolean isDrivingFrom(
      ServerWorld world, BlockPos pos, BlockState state, Hue color, Direction side) {
    return connectsFrom(state, side) && isDriving(world, pos, state, color);
  }

  default boolean holdsLook() {
    return false;
  }

  default void onWireChange(ServerWorld world, BlockPos pos, BlockState state) {}

  default int load(ServerWorld world, BlockPos pos, BlockState state) {
    return 0;
  }

  default int ratedLoad(PowerSettings settings) {
    return 0;
  }

  default int loadWhenWired(ServerWorld world, BlockPos pos, BlockState state) {
    WireEngine engine = WireEngine.of(world);
    boolean wired = cells(pos, state).stream().anyMatch(engine::isWired);
    return wired ? ratedLoad(Hardwired.SERVER_CONFIG.power()) : 0;
  }

  default List<BlockPos> cells(BlockPos pos, BlockState state) {
    return List.of(pos);
  }

  default BlockPos loadAnchor(BlockPos pos, BlockState state) {
    return cells(pos, state).getFirst();
  }
}
