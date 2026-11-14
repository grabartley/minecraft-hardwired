package com.grahambartley.hardwired.phone;

import net.minecraft.server.world.ServerWorld;

public final class PhoneNetwork {

  private static final int REFRESH_TICKS = 20;

  private PhoneNetwork() {}

  public static void tick(ServerWorld world) {
    PhoneDirectory directory = PhoneDirectory.of(world);
    if (world.getTime() % REFRESH_TICKS == 0) {
      directory.refresh();
    } else {
      directory.refreshIfStale();
    }
    Switchboard.of(world).tick();
  }
}
