package com.grahambartley.hardwired.client.glow;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.ChunkLightProvider;

final class WorldGlowSurfaces implements GlowSurfaces {

  private static final float NOW = 1F;

  private final ClientWorld world;
  private final float skyBrightness;

  WorldGlowSurfaces(ClientWorld world) {
    this.world = world;
    this.skyBrightness = world.getSkyBrightness(NOW);
  }

  @Override
  public boolean isSolid(BlockPos pos) {
    return world.getBlockState(pos).isOpaqueFullCube(EmptyBlockView.INSTANCE, pos);
  }

  @Override
  public int cost(BlockPos from, Direction side) {
    BlockPos to = from.offset(side);
    BlockState toState = world.getBlockState(to);
    return Math.max(
        1,
        ChunkLightProvider.getRealisticOpacity(
            EmptyBlockView.INSTANCE,
            world.getBlockState(from),
            from,
            toState,
            to,
            side,
            toState.getOpacity(EmptyBlockView.INSTANCE, to)));
  }

  @Override
  public List<Box> partBoxes(BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (!PartShapes.isPlainModel(state.getRenderType(), state.hasBlockEntity())
        || state.getCollisionShape(EmptyBlockView.INSTANCE, pos).isEmpty()) {
      return List.of();
    }
    return state.getOutlineShape(EmptyBlockView.INSTANCE, pos).getBoundingBoxes();
  }

  @Override
  public boolean showsSide(BlockPos pos, Direction side) {
    return Block.shouldDrawSide(world.getBlockState(pos), world, pos, side, pos.offset(side));
  }

  @Override
  public float daylight(BlockPos pos) {
    return GlowColor.daylight(world.getLightLevel(LightType.SKY, pos), skyBrightness);
  }
}
