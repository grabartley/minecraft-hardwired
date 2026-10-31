package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.advancement.ModCriteria;
import com.grahambartley.hardwired.block.CircuitBoardBlockEntity;
import com.grahambartley.hardwired.circuit.Cost;
import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Refusal;
import com.grahambartley.hardwired.circuit.Validator;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.grahambartley.hardwired.network.CircuitActionPayload;
import com.grahambartley.hardwired.network.CircuitResultPayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.registry.ModComponents;
import java.util.Optional;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class CircuitActions {

  private static final int MIN_TICKS_BETWEEN_ACTIONS = 4;
  private static final String SAVED = "screen.hardwired.circuit.saved";
  private static final String PASTED = "screen.hardwired.circuit.pasted";
  private static final String LOADED = "screen.hardwired.circuit.loaded";
  private static final String MADE = "screen.hardwired.circuit.made";
  private static final ActionRate RATE = new ActionRate(MIN_TICKS_BETWEEN_ACTIONS);

  private CircuitActions() {}

  public static void forget(ServerPlayerEntity player) {
    RATE.forget(player.getUuid());
  }

  public static void act(ServerPlayerEntity player, CircuitActionPayload action) {
    BlockPos pos = action.pos();
    CircuitAction kind = CircuitAction.byIndex(action.action());
    if (kind == null
        || !player.canModifyBlocks()
        || !DeviceActions.canUse(player, pos)
        || !(player.getServerWorld().getBlockEntity(pos)
            instanceof CircuitBoardBlockEntity board)) {
      return;
    }
    if (!RATE.allow(player.getUuid(), player.getServer().getTicks())) {
      refuse(player, pos, Refusal.TOO_FAST, 0);
      return;
    }
    switch (kind) {
      case SAVE -> store(player, board, action.text(), SAVED);
      case PASTE -> store(player, board, action.text(), PASTED);
      case LOAD -> load(player, board);
      case MAKE -> make(player, board);
    }
  }

  private static void store(
      ServerPlayerEntity player, CircuitBoardBlockEntity board, String text, String message) {
    switch (DesignText.parse(text, settings().maxParts())) {
      case DesignText.Result.Refused refused -> refuse(player, board.getPos(), refused.reason(), 0);
      case DesignText.Result.Accepted accepted -> {
        board.setDesign(accepted.design(), false);
        if (!accepted.design().isEmpty()) {
          ModCriteria.DEVICE.trigger(player, ModCriteria.CIRCUIT_SAVED);
        }
        accept(player, board, message);
      }
    }
  }

  private static void load(ServerPlayerEntity player, CircuitBoardBlockEntity board) {
    Optional<Design> held = heldDesign(player);
    if (held.isEmpty()) {
      refuse(player, board.getPos(), Refusal.NOT_HOLDING_CIRCUIT, 0);
      return;
    }
    Optional<Refusal> refusal = Validator.check(held.get(), settings().maxParts());
    if (refusal.isPresent()) {
      refuse(player, board.getPos(), refusal.get(), 0);
      return;
    }
    board.setDesign(held.get(), true);
    accept(player, board, LOADED);
  }

  private static Optional<Design> heldDesign(ServerPlayerEntity player) {
    for (Hand hand : Hand.values()) {
      ItemStack stack = player.getStackInHand(hand);
      if (stack.isOf(ModBlocks.CIRCUIT_BLOCK.asItem())) {
        Design design = stack.get(ModComponents.CIRCUIT_DESIGN);
        if (design != null) {
          return Optional.of(design);
        }
      }
    }
    return Optional.empty();
  }

  private static void make(ServerPlayerEntity player, CircuitBoardBlockEntity board) {
    BlockPos pos = board.getPos();
    Design design = board.design();
    if (design.isEmpty()) {
      refuse(player, pos, Refusal.EMPTY, 0);
      return;
    }
    CircuitSettings settings = settings();
    Optional<Refusal> refusal = Validator.check(design, settings.maxParts());
    if (refusal.isPresent()) {
      refuse(player, pos, refusal.get(), 0);
      return;
    }
    int redstone = Cost.redstone(design.parts().size(), settings.partsPerRedstone());
    if (!player.isCreative() && !pay(player, pos, redstone)) {
      return;
    }
    ItemStack made = new ItemStack(ModBlocks.CIRCUIT_BLOCK);
    made.set(ModComponents.CIRCUIT_DESIGN, design);
    player.getInventory().offerOrDrop(made);
    ModCriteria.DEVICE.trigger(player, ModCriteria.CIRCUIT_MADE);
    if (board.isCopy()) {
      ModCriteria.DEVICE.trigger(player, ModCriteria.CIRCUIT_COPIED);
    }
    accept(player, board, MADE);
  }

  private static boolean pay(ServerPlayerEntity player, BlockPos pos, int redstone) {
    PlayerInventory inventory = player.getInventory();
    Item blank = ModBlocks.BLANK_CIRCUIT_BLOCK.asItem();
    if (inventory.count(blank) < 1) {
      refuse(player, pos, Refusal.NEEDS_BLANK, 1);
      return false;
    }
    if (inventory.count(Items.REDSTONE) < redstone) {
      refuse(player, pos, Refusal.NEEDS_REDSTONE, redstone);
      return false;
    }
    take(player, stack -> stack.isOf(blank), 1);
    take(player, stack -> stack.isOf(Items.REDSTONE), redstone);
    return true;
  }

  private static void take(ServerPlayerEntity player, Predicate<ItemStack> item, int count) {
    player.getInventory().remove(item, count, player.playerScreenHandler.getCraftingInput());
  }

  private static CircuitSettings settings() {
    return Hardwired.SERVER_CONFIG.circuit();
  }

  private static void accept(
      ServerPlayerEntity player, CircuitBoardBlockEntity board, String message) {
    ServerPlayNetworking.send(
        player,
        new CircuitResultPayload(board.getPos(), DesignText.encode(board.design()), message, 0));
  }

  private static void refuse(ServerPlayerEntity player, BlockPos pos, Refusal reason, int amount) {
    ServerPlayNetworking.send(player, new CircuitResultPayload(pos, "", reason.key(), amount));
  }
}
