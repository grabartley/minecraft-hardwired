package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.lift.LiftCarEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin {

  @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
  private void hardwired$stayInLiftCar(CallbackInfoReturnable<Boolean> callback) {
    if (((PlayerEntity) (Object) this).getVehicle() instanceof LiftCarEntity) {
      callback.setReturnValue(false);
    }
  }
}
