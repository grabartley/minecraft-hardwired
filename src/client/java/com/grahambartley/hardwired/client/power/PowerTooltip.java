package com.grahambartley.hardwired.client.power;

import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.power.PowerLabel;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.BlockItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PowerTooltip {

  private PowerTooltip() {}

  public static void register() {
    ItemTooltipCallback.EVENT.register(
        (stack, context, type, lines) -> {
          if (stack.getItem() instanceof BlockItem item) {
            PowerLabel.of(item.getBlock(), SyncedServerConfig.get().power())
                .ifPresent(
                    label ->
                        lines.add(
                            Text.translatable(label.key(), label.amount())
                                .formatted(Formatting.GRAY)));
          }
        });
  }
}
