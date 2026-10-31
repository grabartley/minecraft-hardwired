package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.lift.LiftCarEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin {

  @Inject(method = "isEntityOnAir", at = @At("RETURN"), cancellable = true)
  private void hardwired$liftCarIsGround(Entity entity, CallbackInfoReturnable<Boolean> callback) {
    if (callback.getReturnValueZ() && LiftCarEntity.holdsUp(entity)) {
      callback.setReturnValue(false);
    }
  }
}
