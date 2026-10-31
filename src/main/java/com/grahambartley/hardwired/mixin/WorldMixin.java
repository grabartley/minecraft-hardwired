package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.VanillaPower;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public abstract class WorldMixin implements RedstoneView {

  @Override
  public int getEmittedRedstonePower(BlockPos pos, Direction direction) {
    if ((Object) this instanceof ServerWorld world && VanillaPower.drives(world, pos, direction)) {
      return 15;
    }
    return RedstoneView.super.getEmittedRedstonePower(pos, direction);
  }
}
