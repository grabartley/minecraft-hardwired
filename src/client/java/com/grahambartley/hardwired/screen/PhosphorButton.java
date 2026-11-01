package com.grahambartley.hardwired.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class PhosphorButton extends ButtonWidget {

  private static final int FONT_HEIGHT = 8;
  private static final int MARGIN = 2;

  public PhosphorButton(int x, int y, int width, int height, Text label, Runnable action) {
    super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION_SUPPLIER);
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    int ink =
        !active
            ? PhosphorScreen.GRID_DOT
            : isSelected() ? PhosphorScreen.BRIGHT : PhosphorScreen.DIM;
    int right = getX() + width;
    int bottom = getY() + height;
    context.fill(getX(), getY(), right, bottom, ink);
    context.fill(getX() + 1, getY() + 1, right - 1, bottom - 1, PhosphorScreen.GLASS);
    TextRenderer font = MinecraftClient.getInstance().textRenderer;
    PanelText.flat(
        context,
        font,
        getMessage(),
        getX() + MARGIN,
        right - MARGIN,
        getY() + (height - FONT_HEIGHT + 1) / 2,
        active ? PhosphorScreen.BRIGHT : PhosphorScreen.DIM,
        PanelText.Align.CENTER);
  }
}
