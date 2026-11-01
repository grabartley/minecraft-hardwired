package com.grahambartley.hardwired.client.glow;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.LightType;

final class WorldGlowSurfaces implements GlowSurfaces {

  private final ClientWorld world;
  private final int ambientDarkness;

  WorldGlowSurfaces(ClientWorld world) {
    this.world = world;
    this.ambientDarkness = world.getAmbientDarkness();
  }

  @Override
  public boolean isSolid(BlockPos pos) {
    return world.getBlockState(pos).isOpaqueFullCube(EmptyBlockView.INSTANCE, pos);
  }

  @Override
  public List<Box> partBoxes(BlockPos pos) {
    BlockState state = world.getBlockState(pos);
    return PartShapes.takesTint(
            state.getRenderType(),
            state.hasBlockEntity(),
            state.getCollisionShape(EmptyBlockView.INSTANCE, pos))
        ? state.getOutlineShape(EmptyBlockView.INSTANCE, pos).getBoundingBoxes()
        : List.of();
  }

  @Override
  public float daylight(BlockPos pos) {
    return GlowColor.daylight(world.getLightLevel(LightType.SKY, pos), ambientDarkness);
  }
}
