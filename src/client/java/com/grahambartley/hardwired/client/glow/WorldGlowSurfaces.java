package com.grahambartley.hardwired.client.glow;

import java.util.List;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.LightType;

final class WorldGlowSurfaces implements GlowSurfaces {

  private static final float MAX_SKY = 15F;
  private static final float DARKEST = 11F;

  private final ClientWorld world;
  private final float dayness;

  WorldGlowSurfaces(ClientWorld world) {
    this.world = world;
    this.dayness = 1F - world.getAmbientDarkness() / DARKEST;
  }

  @Override
  public boolean isSolid(BlockPos pos) {
    return world.getBlockState(pos).isOpaqueFullCube(world, pos);
  }

  @Override
  public List<Box> partBoxes(BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    if (state.isAir() || state.hasBlockEntity() || state.getRenderType() != BlockRenderType.MODEL) {
      return List.of();
    }
    VoxelShape outline = state.getOutlineShape(world, pos);
    return PartShapes.sameShape(outline, state.getCollisionShape(world, pos))
        ? outline.getBoundingBoxes()
        : List.of();
  }

  @Override
  public float daylight(BlockPos pos) {
    return world.getLightLevel(LightType.SKY, pos) / MAX_SKY * dayness;
  }
}
