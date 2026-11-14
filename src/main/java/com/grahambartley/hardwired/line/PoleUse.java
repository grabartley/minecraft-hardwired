package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.block.TelephonePoleBlock;
import com.grahambartley.hardwired.item.WireItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PoleUse {

  private PoleUse() {}

  public static ActionResult onUseBlock(
      PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
    BlockPos pos = hit.getBlockPos();
    if (!TelephonePoleBlock.isPole(world.getBlockState(pos))) {
      return ActionResult.PASS;
    }
    ItemStack held = player.getStackInHand(hand);
    boolean stringing = held.getItem() instanceof WireItem && WireItem.canLay(player);
    boolean waxing = held.isOf(Items.HONEYCOMB) && !WireItem.isHolding(player);
    if (!stringing && !waxing) {
      return ActionResult.PASS;
    }
    if (!(player instanceof ServerPlayerEntity server)) {
      return stringing && TelephonePoleBlock.isTop(world, pos)
          ? ActionResult.SUCCESS
          : ActionResult.PASS;
    }
    boolean used =
        stringing ? PoleActions.use(server, pos, held) : PoleActions.wax(server, pos, held);
    return used ? ActionResult.SUCCESS : ActionResult.PASS;
  }
}
