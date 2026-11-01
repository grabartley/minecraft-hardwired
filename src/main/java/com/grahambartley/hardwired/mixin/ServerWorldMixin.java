package com.grahambartley.hardwired.mixin;

import com.grahambartley.hardwired.radio.StationRegistry;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WiredWorld;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements WiredWorld {

  @Unique private WireEngine hardwired$engine;
  @Unique private final StationRegistry hardwired$stations = new StationRegistry();

  @Override
  public WireEngine hardwired$wires() {
    if (hardwired$engine == null) {
      hardwired$engine = new WireEngine((ServerWorld) (Object) this);
    }
    return hardwired$engine;
  }

  @Override
  public StationRegistry hardwired$stations() {
    return hardwired$stations;
  }
}
