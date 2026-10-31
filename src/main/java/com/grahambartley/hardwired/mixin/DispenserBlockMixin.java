package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.VanillaPower;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {

  @Inject(method = "neighborUpdate", at = @At("HEAD"))
  private void hardwired$guardQuasiConnectivity(
      BlockState state,
      World world,
      BlockPos pos,
      Block sourceBlock,
      BlockPos sourcePos,
      boolean notify,
      CallbackInfo ci) {
    VanillaPower.guardQuasiConnectivity(pos);
  }

  @Inject(method = "neighborUpdate", at = @At("RETURN"))
  private void hardwired$clearQuasiGuard(
      BlockState state,
      World world,
      BlockPos pos,
      Block sourceBlock,
      BlockPos sourcePos,
      boolean notify,
      CallbackInfo ci) {
    VanillaPower.clearQuasiGuard();
  }
}
