package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {

  @Inject(method = "neighborUpdate", at = @At("HEAD"))
  private void hardwired$onNeighbourUpdate(
      World world,
      BlockPos pos,
      Block sourceBlock,
      BlockPos sourcePos,
      boolean notify,
      CallbackInfo ci) {
    if (world instanceof ServerWorld serverWorld) {
      WireEngine engine = WireEngine.of(serverWorld);
      if (engine.grid().isTouched()) {
        engine.refresh(pos);
      }
    }
  }
}
