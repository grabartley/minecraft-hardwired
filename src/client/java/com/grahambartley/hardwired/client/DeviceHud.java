package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.block.ElectronicBlockEntity;
import com.grahambartley.hardwired.block.MasterSwitchBlockEntity;
import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.network.DeviceInspectResultPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
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
    if (client.options.hudHidden || client.currentScreen != null) {
      return;
    }
    List<Text> lines = lines(aimedAt(client));
    int y = context.getScaledWindowHeight() / 2 + BELOW_CROSSHAIR;
    for (Text line : lines) {
      context.drawCenteredTextWithShadow(
          client.textRenderer, line, context.getScaledWindowWidth() / 2, y, WHITE);
      y += client.textRenderer.fontHeight + 1;
    }
  }

  static BlockEntity aimedAt(MinecraftClient client) {
    if (client.world == null
        || client.crosshairTarget == null
        || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
      return null;
    }
    BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
    BlockState state = client.world.getBlockState(pos);
    return client.world.getBlockEntity(
        state.getBlock() instanceof WireDevice device ? device.loadAnchor(pos, state) : pos);
  }

  private static List<Text> lines(BlockEntity entity) {
    if (entity instanceof ElectronicBlockEntity device && device.status().isStopped()) {
      return List.of(Text.translatable(device.status().translationKey()));
    }
    DeviceInspectResultPayload reading = entity == null ? null : DeviceReadings.at(entity);
    if (reading == null) {
      return List.of();
    }
    List<Text> lines = new ArrayList<>();
    if (reading.status().isStopped() && !reading.overloaded()) {
      lines.add(Text.translatable(reading.status().translationKey()));
    }
    if (entity instanceof SourceBlockEntity || entity instanceof MasterSwitchBlockEntity) {
      lines.add(
          Text.translatable(
              reading.overloaded() ? "hud.hardwired.overloaded" : "hud.hardwired.load",
              reading.load(),
              reading.supply()));
    }
    return lines;
  }
}
