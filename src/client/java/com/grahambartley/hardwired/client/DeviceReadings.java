package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.network.DeviceInspectPayload;
import com.grahambartley.hardwired.network.DeviceInspectResultPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;

public final class DeviceReadings {

  private static final int INTERVAL = 10;
  private static final int MAX_AGE = INTERVAL * 3;

  private static DeviceInspectResultPayload latest;
  private static int age;

  private DeviceReadings() {}

  public static void register() {
    ClientPlayNetworking.registerGlobalReceiver(
        DeviceInspectResultPayload.ID,
        (payload, context) -> {
          latest = payload;
          age = 0;
        });
    ClientTickEvents.END_CLIENT_TICK.register(DeviceReadings::tick);
  }

  private static void tick(MinecraftClient client) {
    if (++age > MAX_AGE) {
      latest = null;
    }
    BlockEntity entity = DeviceHud.aimedAt(client);
    if (reportsStops(entity) && age % INTERVAL == 0) {
      ClientPlayNetworking.send(new DeviceInspectPayload(entity.getPos()));
    }
  }

  static boolean showsGridTotals(BlockEntity entity) {
    return entity.getCachedState().getBlock() instanceof WireDevice device
        && device.showsGridTotals();
  }

  private static boolean reportsStops(BlockEntity entity) {
    return entity != null
        && entity.getCachedState().getBlock() instanceof WireDevice device
        && !(entity instanceof ElectronicBlockEntity)
        && (device.showsGridTotals() || device.ratedLoad(SyncedServerConfig.get().power()) > 0);
  }

  static DeviceInspectResultPayload at(BlockEntity entity) {
    return latest != null && latest.pos().equals(entity.getPos()) ? latest : null;
  }
}
