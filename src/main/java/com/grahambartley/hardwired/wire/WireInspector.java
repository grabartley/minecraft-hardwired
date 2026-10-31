package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireInspectPayload;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WireInspector {

  private WireInspector() {}

  public static void inspect(ServerPlayerEntity player, WireInspectPayload request) {
    if (!WireItem.isHolding(player)) {
      return;
    }
    WireEngine engine = WireEngine.of(player.getServerWorld());
    Hue color = Hue.byIndex(request.color());
    WireNetwork network = engine.grid().networkOf(new WireCell(request.face(), color));
    if (network == null) {
      return;
    }
    List<String> drivers = new ArrayList<>();
    for (TouchPoint touch : network.touches()) {
      if (drivers.size() < WireInspectResultPayload.MAX_DRIVERS
          && engine.isLoaded(touch.pos())
          && engine.isInput(touch, color)) {
        drivers.add(
            player.getServerWorld().getBlockState(touch.pos()).getBlock().getTranslationKey());
      }
    }
    ServerPlayNetworking.send(
        player,
        new WireInspectResultPayload(
            color.ordinal(), network.isOn(), network.isTripped(), drivers));
  }
}
