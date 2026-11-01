package com.grahambartley.hardwired.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

final class PanelText {

  enum Align {
    LEFT,
    CENTER,
    RIGHT
  }

  private static final int FONT_HEIGHT = 9;
  private static final int SHADOW_ROOM = 1;
  private static final double MILLIS_PER_PIXEL = 30D;
  private static final double SHORTEST_SWEEP_MILLIS = 3000D;

  private PanelText() {}

  static void shadowed(
      DrawContext context,
      TextRenderer font,
      Text text,
      int left,
      int right,
      int y,
      int color,
      Align align) {
    draw(context, font, text, left, right, y, color, true, align);
  }

  static void flat(
      DrawContext context,
      TextRenderer font,
      Text text,
      int left,
      int right,
      int y,
      int color,
      Align align) {
    draw(context, font, text, left, right, y, color, false, align);
  }

  private static void draw(
      DrawContext context,
      TextRenderer font,
      Text text,
      int left,
      int right,
      int y,
      int color,
      boolean shadow,
      Align align) {
    int textWidth = font.getWidth(text);
    int overflow = overflow(textWidth, right - left);
    if (overflow == 0) {
      context.drawText(font, text, startX(left, right, textWidth, align), y, color, shadow);
      return;
    }
    context.enableScissor(left, y - SHADOW_ROOM, right, y + FONT_HEIGHT + SHADOW_ROOM);
    int x = left - scrollOffset(overflow, Util.getMeasuringTimeMs());
    context.drawText(font, text, x, y, color, shadow);
    context.disableScissor();
  }

  static int overflow(int textWidth, int space) {
    return Math.max(textWidth - space, 0);
  }

  static int startX(int left, int right, int textWidth, Align align) {
    return switch (align) {
      case LEFT -> left;
      case CENTER -> left + (right - left - textWidth) / 2;
      case RIGHT -> right - textWidth;
    };
  }

  static int scrollOffset(int overflow, long millis) {
    double sweep = Math.max(overflow * MILLIS_PER_PIXEL, SHORTEST_SWEEP_MILLIS) * 2;
    double wave = Math.cos(2 * Math.PI * millis / sweep);
    double eased = (1 - Math.sin(Math.PI / 2 * wave)) / 2;
    return (int) Math.round(eased * overflow);
  }
}
