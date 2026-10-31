package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.lift.LiftCarEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
abstract class PlayerManagerMixin {

  @Inject(method = "remove", at = @At("HEAD"))
  private void hardwired$leaveLiftCarSafely(ServerPlayerEntity player, CallbackInfo callback) {
    LiftCarEntity.riderLeaving(player);
  }
}
