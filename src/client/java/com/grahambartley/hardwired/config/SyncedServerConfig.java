package com.grahambartley.hardwired.config;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SyncedServerConfig {

  private static volatile HardwiredConfig current = HardwiredConfig.defaults();

  private SyncedServerConfig() {}

  public static HardwiredConfig get() {
    return current;
  }

  public static void apply(final HardwiredConfig config) {
    current = config;
  }

  public static void resetOnDisconnect() {
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
  }

  public static void reset() {
    current = HardwiredConfig.defaults();
  }
}
