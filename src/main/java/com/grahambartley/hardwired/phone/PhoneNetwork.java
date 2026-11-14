package com.grahambartley.hardwired.phone;

import net.minecraft.server.world.ServerWorld;

public final class PhoneNetwork {

  private static final int REFRESH_TICKS = 20;

  private PhoneNetwork() {}

  public static void tick(ServerWorld world) {
    if (world.getTime() % REFRESH_TICKS == 0) {
      PhoneDirectory.of(world).refresh();
    }
    Switchboard.of(world).tick();
  }
}
