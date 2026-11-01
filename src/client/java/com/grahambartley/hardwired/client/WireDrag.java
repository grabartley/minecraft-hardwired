package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.network.WireInspectPayload;
import com.grahambartley.hardwired.network.WireInspectResultPayload;
import com.grahambartley.hardwired.network.WireLayPayload;
import com.grahambartley.hardwired.network.WireRemovePayload;
import com.grahambartley.hardwired.wire.WireFace;
import com.grahambartley.hardwired.wire.WirePath;
import com.grahambartley.hardwired.wire.WireRules;
import com.grahambartley.hardwired.wire.WireWax;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public final class WireDrag {

  private static final int MAX_SEARCH = 400;
  private static final int INSPECT_INTERVAL = 10;
  private static final int RESULT_LIFETIME = INSPECT_INTERVAL * 2;

  private static WireFace lastLaid;
  private static boolean removedThisPress;
  private static int ticks;
  private static WireInspectResultPayload result;
  private static int resultAge;

  private WireDrag() {}

  public static WireInspectResultPayload inspected() {
    return resultAge <= RESULT_LIFETIME ? result : null;
  }

  public static void acceptResult(WireInspectResultPayload payload) {
    result = payload;
    resultAge = 0;
  }

  public static ActionResult onUseBlock(
      PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (!world.isClient) {
      return ActionResult.PASS;
    }
    if (isWaxing(player, hit)) {
      return ActionResult.SUCCESS;
    }
    if (!WireItem.canLay(player)) {
      return ActionResult.PASS;
    }
    boolean claimed =
        player.isSneaking() || WireRules.accepts(world, hit.getBlockPos(), hit.getSide());
    return claimed ? ActionResult.FAIL : ActionResult.PASS;
  }

  public static void tick(MinecraftClient client) {
    resultAge++;
    ClientPlayerEntity player = client.player;
    if (player == null || !WireItem.isHolding(player)) {
      lastLaid = null;
      return;
    }
    if (!(client.crosshairTarget instanceof BlockHitResult hit)
        || hit.getType() != HitResult.Type.BLOCK) {
      release();
      return;
    }
    inspect(hit);
    if (!client.options.useKey.isPressed() || !WireItem.canLay(player) || isWaxing(player, hit)) {
      release();
    } else if (player.isSneaking()) {
      remove(hit);
    } else {
      lay(client, hit);
    }
  }

  private static boolean isWaxing(PlayerEntity player, BlockHitResult hit) {
    return WireWax.of(player.getMainHandStack(), player.getOffHandStack(), player.isSneaking())
        .filter(
            use ->
                use.action() == WireWax.Action.SEAL
                    ? WireViewStore.wiredFaceAt(hit.getBlockPos(), hit.getSide()).isPresent()
                    : WireViewStore.hasSealedCellAt(hit.getBlockPos(), hit.getSide()))
        .isPresent();
  }

  private static void release() {
    lastLaid = null;
    removedThisPress = false;
  }

  private static void remove(BlockHitResult hit) {
    if (!removedThisPress) {
      removedThisPress = true;
      WireViewStore.wiredFaceAt(hit.getBlockPos(), hit.getSide())
          .ifPresent(face -> ClientPlayNetworking.send(new WireRemovePayload(face)));
    }
  }

  private static void lay(MinecraftClient client, BlockHitResult hit) {
    WireFace face = new WireFace(hit.getBlockPos(), hit.getSide());
    World world = client.world;
    if (face.equals(lastLaid) || !WireRules.accepts(world, face.pos(), face.side())) {
      return;
    }
    List<WireFace> path =
        lastLaid == null
            ? List.of(face)
            : WirePath.between(
                lastLaid,
                face,
                next -> WireRules.accepts(world, next.pos(), next.side()),
                MAX_SEARCH);
    ClientPlayNetworking.send(
        new WireLayPayload(path.subList(0, Math.min(path.size(), WireLayPayload.MAX_FACES))));
    if (lastLaid == null) {
      client.player.swingHand(Hand.MAIN_HAND);
    }
    lastLaid = face;
  }

  private static void inspect(BlockHitResult hit) {
    if (++ticks % INSPECT_INTERVAL == 0) {
      WireViewStore.wiredFaceAt(hit.getBlockPos(), hit.getSide())
          .ifPresent(
              face ->
                  WireLanes.nearest(WireViewStore.cellsOn(face), hit.getPos())
                      .ifPresent(
                          cell ->
                              ClientPlayNetworking.send(
                                  new WireInspectPayload(face, cell.color()))));
    }
  }
}
