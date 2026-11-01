package com.grahambartley.hardwired.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class RedButton extends ButtonWidget {

  private static final int LIT = 0xFFD81E1E;
  private static final int DARK = 0xFF5C1212;
  private static final int WHITE = 0xFFFFFFFF;
  private static final int BORDER = 1;

  private boolean lit;

  RedButton(int x, int y, int width, int height, Text label, PressAction action) {
    super(x, y, width, height, label, action, DEFAULT_NARRATION_SUPPLIER);
  }

  void setLit(boolean next) {
    lit = next;
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    int right = getX() + getWidth();
    int bottom = getY() + getHeight();
    context.fill(getX(), getY(), right, bottom, SlatePlate.BRASS);
    context.fill(
        getX() + BORDER, getY() + BORDER, right - BORDER, bottom - BORDER, lit ? LIT : DARK);
    context.drawCenteredTextWithShadow(
        MinecraftClient.getInstance().textRenderer,
        getMessage(),
        getX() + getWidth() / 2,
        getY() + (getHeight() - 8) / 2,
        WHITE);
  }
}
