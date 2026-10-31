package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WiredWorld;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements WiredWorld {

  @Unique private WireEngine hardwired$engine;

  @Override
  public WireEngine hardwired$wires() {
    if (hardwired$engine == null) {
      hardwired$engine = new WireEngine((ServerWorld) (Object) this);
    }
    return hardwired$engine;
  }
}
