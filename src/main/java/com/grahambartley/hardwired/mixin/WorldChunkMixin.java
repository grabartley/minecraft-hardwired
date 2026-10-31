package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {

  @Shadow @Final World world;

  @Inject(method = "setBlockState", at = @At("RETURN"))
  private void hardwired$onBlockChanged(
      BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
    if (cir.getReturnValue() != null && world instanceof ServerWorld serverWorld) {
      WireEngine engine = WireEngine.of(serverWorld);
      if (engine.grid().isTouched()) {
        engine.onBlockChanged(pos, cir.getReturnValue(), state);
      }
    }
  }
}
