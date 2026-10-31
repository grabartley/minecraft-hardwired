package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireInspectPayload;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class WireInspector {

  private WireInspector() {}

  public static void inspect(ServerPlayerEntity player, WireInspectPayload request) {
    if (!WireItem.isHolding(player) || !WireLaying.inReach(player, request.face())) {
      return;
    }
    WireEngine engine = WireEngine.of(player.getServerWorld());
    Hue color = Hue.byIndex(request.color());
    WireNetwork network = engine.grid().networkOf(new WireCell(request.face(), color));
    if (network == null) {
      return;
    }
    List<String> drivers =
        Arrays.stream(network.rememberedInputs().toLongArray())
            .sorted()
            .mapToObj(BlockPos::fromLong)
            .filter(engine::isLoaded)
            .limit(WireInspectResultPayload.MAX_DRIVERS)
            .map(pos -> player.getServerWorld().getBlockState(pos).getBlock().getTranslationKey())
            .toList();
    ServerPlayNetworking.send(
        player,
        new WireInspectResultPayload(
            color.ordinal(), network.isOn(), network.isTripped(), drivers));
  }
}
