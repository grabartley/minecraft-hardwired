package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
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
    if ((entity instanceof SourceBlockEntity || entity instanceof MasterSwitchBlockEntity)
        && age % INTERVAL == 0) {
      ClientPlayNetworking.send(new DeviceInspectPayload(entity.getPos()));
    }
  }

  static DeviceInspectResultPayload at(BlockEntity entity) {
    return latest != null && latest.pos().equals(entity.getPos()) ? latest : null;
  }
}
