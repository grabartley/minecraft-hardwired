package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.MasterSwitchBlock;
import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.power.GridState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class DeviceHud {

  private static final int BELOW_CROSSHAIR = 12;
  private static final int WHITE = 0xFFFFFFFF;

  private DeviceHud() {}

  public static void render(DrawContext context, RenderTickCounter tick) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.world == null
        || client.options.hudHidden
        || client.currentScreen != null
        || client.crosshairTarget == null
        || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
      return;
    }
    BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
    BlockEntity entity = client.world.getBlockEntity(pos);
    if (entity == null && client.world.getBlockState(pos).getBlock() instanceof MasterSwitchBlock) {
      entity = client.world.getBlockEntity(pos.down());
    }
    Text line = line(entity);
    if (line != null) {
      context.drawCenteredTextWithShadow(
          client.textRenderer,
          line,
          context.getScaledWindowWidth() / 2,
          context.getScaledWindowHeight() / 2 + BELOW_CROSSHAIR,
          WHITE);
    }
  }

  private static Text line(BlockEntity entity) {
    if (entity instanceof ElectronicBlockEntity device && device.status().isStopped()) {
      return Text.translatable(device.status().translationKey());
    }
    if (entity instanceof SourceBlockEntity source) {
      return totals(source.load(), source.supply(), source.grid() == GridState.OVERLOADED);
    }
    if (entity instanceof MasterSwitchBlockEntity master) {
      return totals(master.load(), master.supply(), master.load() > master.supply());
    }
    return null;
  }

  private static Text totals(int load, int supply, boolean overloaded) {
    return Text.translatable(
        overloaded ? "hud.hardwired.overloaded" : "hud.hardwired.load", load, supply);
  }
}
