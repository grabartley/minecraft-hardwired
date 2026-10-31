package com.grahambartley.hardwired.item;

import com.grahambartley.hardwired.wire.Hue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

public final class WireItem extends Item {

  private final Hue color;

  public WireItem(Hue color, Settings settings) {
    super(settings);
    this.color = color;
  }

  public Hue color() {
    return color;
  }

  public static boolean isHolding(PlayerEntity player) {
    return player.getMainHandStack().getItem() instanceof WireItem
        || player.getOffHandStack().getItem() instanceof WireItem;
  }

  public static Hand wireHand(PlayerEntity player) {
    return player.getMainHandStack().getItem() instanceof WireItem ? Hand.MAIN_HAND : Hand.OFF_HAND;
  }
}
