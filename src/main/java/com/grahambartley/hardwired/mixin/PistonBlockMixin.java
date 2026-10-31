package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.PistonWireMover;
import com.grahambartley.hardwired.wire.VanillaPower;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {

  @Inject(method = "shouldExtend", at = @At("HEAD"))
  private void hardwired$guardQuasiConnectivity(
      RedstoneView world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
    VanillaPower.guardQuasiConnectivity(pos);
  }

  @Inject(method = "shouldExtend", at = @At("RETURN"))
  private void hardwired$clearQuasiGuard(
      RedstoneView world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
    VanillaPower.clearQuasiGuard();
  }

  @WrapOperation(
      method = "move",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/block/piston/PistonHandler;getMovedBlocks()Ljava/util/List;"))
  private List<BlockPos> hardwired$moveWire(
      PistonHandler handler,
      Operation<List<BlockPos>> original,
      @Local(argsOnly = true) World world,
      @Local(argsOnly = true) Direction direction,
      @Local(argsOnly = true) boolean extending) {
    List<BlockPos> moved = original.call(handler);
    if (world instanceof ServerWorld serverWorld) {
      PistonWireMover.move(
          serverWorld,
          handler.getBrokenBlocks(),
          moved,
          extending ? direction : direction.getOpposite());
    }
    return moved;
  }
}
