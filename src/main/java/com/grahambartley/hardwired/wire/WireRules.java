package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.device.WireDevice;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public final class WireRules {

  public static final TagKey<Block> ACCEPTED =
      TagKey.of(RegistryKeys.BLOCK, Identifier.of(Hardwired.MOD_ID, "wire_surfaces"));
  public static final TagKey<Block> REFUSED =
      TagKey.of(RegistryKeys.BLOCK, Identifier.of(Hardwired.MOD_ID, "wire_refused"));

  public static final TagKey<Block> OUTPUTS =
      TagKey.of(RegistryKeys.BLOCK, Identifier.of(Hardwired.MOD_ID, "wire_outputs"));

  private WireRules() {}

  public static boolean isOutput(BlockState state) {
    return state.getBlock() instanceof WireDevice || state.isIn(OUTPUTS);
  }

  public static boolean accepts(BlockView world, BlockPos pos, Direction side) {
    BlockState state = world.getBlockState(pos);
    if (state.isOf(Blocks.MOVING_PISTON)) {
      return true;
    }
    if (state.isAir() || state.isIn(REFUSED) || state.getHardness(world, pos) < 0) {
      return false;
    }
    return state.isIn(ACCEPTED) || state.isSideSolidFullSquare(world, pos, side);
  }
}
