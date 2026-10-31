package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.item.WireItem;
import java.util.Optional;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class WireWax {

  public enum Action {
    SEAL,
    UNSEAL
  }

  public record Held(Hue wire, Action tool) {}

  public record Use(Action action, Hand toolHand, Hue wire) {}

  private WireWax() {}

  public static Optional<Use> of(ItemStack main, ItemStack off, boolean sneaking) {
    return decide(held(main), held(off), sneaking);
  }

  static Optional<Use> decide(Held main, Held off, boolean sneaking) {
    if (sneaking) {
      return Optional.empty();
    }
    if (main.wire() != null && off.tool() != null) {
      return Optional.of(new Use(off.tool(), Hand.OFF_HAND, main.wire()));
    }
    if (off.wire() != null && main.tool() != null) {
      return Optional.of(new Use(main.tool(), Hand.MAIN_HAND, off.wire()));
    }
    return Optional.empty();
  }

  private static Held held(ItemStack stack) {
    if (stack.getItem() instanceof WireItem wire) {
      return new Held(wire.color(), null);
    }
    if (stack.isOf(Items.HONEYCOMB)) {
      return new Held(null, Action.SEAL);
    }
    return new Held(null, stack.getItem() instanceof AxeItem ? Action.UNSEAL : null);
  }
}
