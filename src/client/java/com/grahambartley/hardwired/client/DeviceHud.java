package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class DeviceHud {

  private static final int BELOW_CROSSHAIR = 12;
  private static final int WHITE = 0xFFFFFFFF;

  private DeviceHud() {}

  public static void render(DrawContext context, RenderTickCounter tick) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.world == null
        || client.currentScreen != null
        || client.crosshairTarget == null
        || client.crosshairTarget.getType() != HitResult.Type.BLOCK
        || !(client.world.getBlockEntity(((BlockHitResult) client.crosshairTarget).getBlockPos())
            instanceof ElectronicBlockEntity device)
        || !device.status().isStopped()) {
      return;
    }
    context.drawCenteredTextWithShadow(
        client.textRenderer,
        Text.translatable(device.status().translationKey()),
        context.getScaledWindowWidth() / 2,
        context.getScaledWindowHeight() / 2 + BELOW_CROSSHAIR,
        WHITE);
  }
}
