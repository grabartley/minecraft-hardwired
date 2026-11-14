package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.TelephonePoleBlock;
import com.grahambartley.hardwired.config.LineSettings;
import com.grahambartley.hardwired.item.WireItem;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireSpan;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.List;
import java.util.Optional;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

public final class PoleActions {

  private static final SpanDrafts DRAFTS = new SpanDrafts();

  private PoleActions() {}

  public static boolean use(ServerPlayerEntity player, BlockPos pos, ItemStack held) {
    if (!(held.getItem() instanceof WireItem wire) || !player.canModifyBlocks()) {
      return false;
    }
    ServerWorld world = player.getServerWorld();
    PoleLines lines = PoleLines.of(world);
    BlockPos top = lines.topOf(pos);
    if (!top.equals(pos)) {
      return false;
    }
    if (player.isSneaking()) {
      unstring(player, lines, top, wire.color());
    } else {
      Identifier dimension = world.getRegistryKey().getValue();
      Optional<SpanDrafts.Draft> draft = DRAFTS.of(player.getUuid(), dimension);
      if (draft.isPresent() && draft.get().color() == wire.color()) {
        finish(player, lines, draft.get(), top, wire.color());
      } else {
        DRAFTS.start(player.getUuid(), new SpanDrafts.Draft(dimension, top, wire.color()));
        tell(player, "message.hardwired.span_started");
      }
    }
    return true;
  }

  public static boolean wax(ServerPlayerEntity player, BlockPos pos, ItemStack honeycomb) {
    if (!player.canModifyBlocks()) {
      return false;
    }
    ServerWorld world = player.getServerWorld();
    PoleLines lines = PoleLines.of(world);
    WireViewer viewer = WireViewer.of(player);
    boolean waxed = lines.wax(lines.topOf(pos), viewer::canSee);
    if (waxed) {
      honeycomb.decrementUnlessCreative(1, player);
      world.playSound(null, pos, SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1F, 1F);
      world.syncWorldEvent(null, WorldEvents.BLOCK_WAXED, pos, 0);
    }
    return waxed;
  }

  public static void tick(MinecraftServer server) {
    DRAFTS.dropWhere(
        (id, draft) -> {
          ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
          boolean stale = player == null || !holds(player, draft.color());
          if (stale && player != null) {
            tell(player, "message.hardwired.span_cancelled");
          }
          return stale;
        });
  }

  public static void forget(ServerPlayerEntity player) {
    DRAFTS.end(player.getUuid());
  }

  public static void clear() {
    DRAFTS.clear();
  }

  private static boolean holds(ServerPlayerEntity player, Hue color) {
    return WireItem.canLay(player)
        && player.getStackInHand(WireItem.wireHand(player)).getItem() instanceof WireItem wire
        && wire.color() == color;
  }

  private static void finish(
      ServerPlayerEntity player, PoleLines lines, SpanDrafts.Draft draft, BlockPos end, Hue color) {
    LineSettings settings = Hardwired.SERVER_CONFIG.line();
    boolean standing =
        TelephonePoleBlock.isPole(player.getServerWorld().getBlockState(draft.start()));
    BlockPos start = standing ? lines.topOf(draft.start()) : draft.start();
    boolean creative = player.isCreative();
    WireSpan planned =
        new WireSpan(
            start, end, color, lines.ownerFor(start, end, color, WireViewer.of(player)), 0);
    int cost = SpanRules.cost(planned.length(), settings.spanBlocksPerWire());
    SpanRules.Finish outcome =
        SpanRules.finish(
            new SpanRules.Attempt(
                start.equals(end) || !standing,
                planned.length(),
                settings.poleSpanBlocks(),
                lines.spansAt(start),
                lines.spansAt(end),
                lines.strung(start, end, color),
                lines.sizeAfter(planned),
                Hardwired.SERVER_CONFIG.wire().maxNetworkSize(),
                player
                    .getInventory()
                    .count(player.getStackInHand(WireItem.wireHand(player)).getItem()),
                cost,
                creative));
    DRAFTS.end(player.getUuid());
    switch (outcome) {
      case CANCEL -> tell(player, "message.hardwired.span_cancelled");
      case TOO_FAR ->
          player.sendMessage(
              Text.translatable("message.hardwired.too_far", settings.poleSpanBlocks()), true);
      case POLE_FULL ->
          player.sendMessage(
              Text.translatable("message.hardwired.pole_full", SpanRules.MOST_SPANS), true);
      case ALREADY_STRUNG -> tell(player, "message.hardwired.already_strung");
      case NEEDS_WIRE ->
          player.sendMessage(Text.translatable("message.hardwired.needs_wire", cost), true);
      case NETWORK_FULL -> tell(player, "message.hardwired.network_full");
      case STRUNG ->
          pay(
              player,
              lines,
              new WireSpan(start, end, color, planned.owner(), creative ? 0 : cost, 0, false));
    }
  }

  private static void pay(ServerPlayerEntity player, PoleLines lines, WireSpan span) {
    if (!lines.string(span)) {
      tell(player, "message.hardwired.already_strung");
      return;
    }
    ItemStack held = player.getStackInHand(WireItem.wireHand(player));
    player
        .getInventory()
        .remove(
            stack -> stack.isOf(held.getItem()),
            span.wire(),
            player.playerScreenHandler.getCraftingInput());
    player
        .getServerWorld()
        .playSound(
            null, span.to(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.BLOCKS, 1F, 1F);
    tell(player, "message.hardwired.span_strung");
  }

  private static void unstring(
      ServerPlayerEntity player, PoleLines lines, BlockPos top, Hue color) {
    DRAFTS.end(player.getUuid());
    List<ItemStack> wire = lines.unstring(top, color, WireViewer.of(player));
    if (wire.isEmpty()) {
      tell(player, "message.hardwired.no_span");
      return;
    }
    wire.forEach(stack -> player.getInventory().offerOrDrop(stack));
    player
        .getServerWorld()
        .playSound(null, top, SoundEvents.ENTITY_LEASH_KNOT_BREAK, SoundCategory.BLOCKS, 1F, 1F);
    tell(player, "message.hardwired.span_removed");
  }

  private static void tell(ServerPlayerEntity player, String key) {
    player.sendMessage(Text.translatable(key), true);
  }
}
