package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public final class WireLaying {

  private static final double REACH_MARGIN = 1.5;

  private WireLaying() {}

  public static void lay(ServerPlayerEntity player, List<WireFace> faces) {
    ItemStack stack = player.getStackInHand(WireItem.wireHand(player));
    if (!WireItem.canLay(player) || !(stack.getItem() instanceof WireItem wire)) {
      return;
    }
    ServerWorld world = player.getServerWorld();
    WireEngine engine = WireEngine.of(world);
    WireViewer viewer = WireViewer.of(player);
    boolean laid = false;
    for (WireFace face : faces) {
      if (stack.isEmpty() && !player.isInCreativeMode()) {
        break;
      }
      WireCell cell =
          new WireCell(
              face, wire.color(), viewer.ownerOfNewWire(engine.grid(), face, wire.color()));
      if (engine.grid().contains(cell)
          || !canEdit(player, face)
          || !WireRules.accepts(world, face.pos(), face.side())) {
        continue;
      }
      if (engine.grid().sizeAfterAdding(cell) > Hardwired.SERVER_CONFIG.wire().maxNetworkSize()) {
        player.sendMessage(Text.translatable("message.hardwired.network_full"), true);
        break;
      }
      engine.layJoining(cell);
      stack.decrementUnlessCreative(1, player);
      laid = true;
    }
    if (laid) {
      playSound(world, player, BlockSoundGroup.COPPER.getPlaceSound());
    }
  }

  public static void remove(ServerPlayerEntity player, WireFace face) {
    if (!WireItem.isHolding(player) || !canEdit(player, face)) {
      return;
    }
    WireEngine engine = WireEngine.of(player.getServerWorld());
    WireCell cell = chooseCell(player, engine.grid(), face);
    if (cell == null) {
      return;
    }
    if (engine.grid().networkOf(cell).isOn()) {
      ModCriteria.DEVICE.trigger(player, ModCriteria.SHORT_CIRCUIT);
    }
    engine.cut(cell);
    if (!player.isInCreativeMode()) {
      player.getInventory().offerOrDrop(new ItemStack(ModItems.wire(cell.color())));
    }
    playSound(player.getServerWorld(), player, BlockSoundGroup.COPPER.getBreakSound());
  }

  private static WireCell chooseCell(ServerPlayerEntity player, WireGrid grid, WireFace face) {
    ItemStack held = player.getStackInHand(WireItem.wireHand(player));
    WireViewer viewer = WireViewer.of(player);
    Optional<WireCell> chosen =
        held.getItem() instanceof WireItem wire
            ? viewer.choose(grid, face, wire.color())
            : viewer.visibleOn(grid, face).stream().findFirst();
    return chosen.orElse(null);
  }

  static boolean canEdit(ServerPlayerEntity player, WireFace face) {
    return inReach(player, face)
        && player.canModifyBlocks()
        && player.getServerWorld().canPlayerModifyAt(player, face.pos());
  }

  static boolean inReach(ServerPlayerEntity player, WireFace face) {
    double reach = player.getBlockInteractionRange() + REACH_MARGIN;
    return player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(face.pos())) <= reach * reach;
  }

  private static void playSound(
      ServerWorld world, ServerPlayerEntity player, net.minecraft.sound.SoundEvent sound) {
    world.playSound(null, player.getBlockPos(), sound, SoundCategory.BLOCKS, 0.5F, 1.4F);
  }
}
