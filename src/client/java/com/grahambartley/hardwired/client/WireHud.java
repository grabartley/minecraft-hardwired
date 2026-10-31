package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.network.WireInspectResultPayload;
import com.grahambartley.hardwired.wire.Hue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class WireHud {

  private static final int LINE_HEIGHT = 10;
  private static final int BOTTOM_MARGIN = 72;
  private static final int WHITE = 0xFFFFFFFF;

  private WireHud() {}

  public static void render(DrawContext context, RenderTickCounter tick) {
    WireInspectResultPayload result = WireDrag.inspected();
    MinecraftClient client = MinecraftClient.getInstance();
    if (result == null || client.player == null) {
      return;
    }
    String stateKey =
        result.tripped()
            ? "hud.hardwired.tripped"
            : result.on() ? "hud.hardwired.on" : "hud.hardwired.off";
    Text heading =
        Text.translatable(
            "hud.hardwired.wire",
            Text.translatable(Hue.byIndex(result.color()).wireKey()),
            Text.translatable(stateKey));
    int y = context.getScaledWindowHeight() - BOTTOM_MARGIN - result.drivers().size() * LINE_HEIGHT;
    context.drawCenteredTextWithShadow(
        client.textRenderer, heading, context.getScaledWindowWidth() / 2, y, WHITE);
    for (String driver : result.drivers()) {
      y += LINE_HEIGHT;
      context.drawCenteredTextWithShadow(
          client.textRenderer,
          Text.translatable("hud.hardwired.driver", Text.translatable(driver)),
          context.getScaledWindowWidth() / 2,
          y,
          WHITE);
    }
  }
}
