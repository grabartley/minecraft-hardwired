package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public final class WireSealing {

  private static final int AXE_WEAR = 1;

  private WireSealing() {}

  public static ActionResult use(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
      return ActionResult.PASS;
    }
    return WireWax.of(player.getMainHandStack(), player.getOffHandStack(), player.isSneaking())
        .map(use -> apply(serverPlayer, use, hit))
        .orElse(ActionResult.PASS);
  }

  private static ActionResult apply(
      ServerPlayerEntity player, WireWax.Use use, BlockHitResult hit) {
    WireEngine engine = WireEngine.of(player.getServerWorld());
    return target(player, engine.grid(), use, hit)
        .map(cell -> engine.grid().networkOf(cell))
        .map(
            network ->
                use.action() == WireWax.Action.SEAL
                    ? seal(player, engine, network, use.toolHand())
                    : unseal(player, engine, network, use.toolHand()))
        .orElse(ActionResult.PASS);
  }

  private static Optional<WireCell> target(
      ServerPlayerEntity player, WireGrid grid, WireWax.Use use, BlockHitResult hit) {
    WireViewer viewer = WireViewer.of(player);
    return WireTopology.targets(hit.getBlockPos(), hit.getSide()).stream()
        .filter(face -> WireLaying.canEdit(player, face))
        .flatMap(face -> viewer.waxTarget(grid, face, use).stream())
        .findFirst();
  }

  private static ActionResult seal(
      ServerPlayerEntity player, WireEngine engine, WireNetwork network, Hand toolHand) {
    if (network.isSealed()) {
      player.sendMessage(Text.translatable("message.hardwired.already_sealed"), true);
      return ActionResult.SUCCESS;
    }
    if (!reown(player, engine, network, player.getUuid())) {
      return ActionResult.SUCCESS;
    }
    player.getStackInHand(toolHand).decrementUnlessCreative(1, player);
    finish(player, SoundEvents.ITEM_HONEYCOMB_WAX_ON, "message.hardwired.sealed");
    return ActionResult.SUCCESS;
  }

  private static ActionResult unseal(
      ServerPlayerEntity player, WireEngine engine, WireNetwork network, Hand toolHand) {
    if (!reown(player, engine, network, null)) {
      return ActionResult.SUCCESS;
    }
    player.getStackInHand(toolHand).damage(AXE_WEAR, player, LivingEntity.getSlotForHand(toolHand));
    finish(player, SoundEvents.ITEM_AXE_WAX_OFF, "message.hardwired.unsealed");
    return ActionResult.SUCCESS;
  }

  private static boolean reown(
      ServerPlayerEntity player, WireEngine engine, WireNetwork network, UUID owner) {
    if (engine.grid().sizeAfterReowning(network, owner)
        > Hardwired.SERVER_CONFIG.wire().maxNetworkSize()) {
      player.sendMessage(Text.translatable("message.hardwired.network_full"), true);
      return false;
    }
    engine.reown(network, owner);
    return true;
  }

  private static void finish(ServerPlayerEntity player, SoundEvent sound, String messageKey) {
    player.sendMessage(Text.translatable(messageKey), true);
    player
        .getServerWorld()
        .playSound(null, player.getBlockPos(), sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
  }
}
