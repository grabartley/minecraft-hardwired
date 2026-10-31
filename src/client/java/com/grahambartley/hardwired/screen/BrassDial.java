package com.grahambartley.hardwired.screen;

import net.minecraft.client.gui.DrawContext;

public final class BrassDial {

  private static final int FACE_COLOR = 0xFF1C1A22;
  private static final int NEEDLE_COLOR = 0xFFFFFFFF;

  private BrassDial() {}

  public static void draw(DrawContext context, int x, int y, int radius, int value, int max) {
    context.fill(x - radius + 2, y - radius + 2, x + radius - 1, y + radius - 1, FACE_COLOR);
    for (DialFace.Pixel pixel : DialFace.ring(radius)) {
      context.fill(
          x + pixel.x(), y + pixel.y(), x + pixel.x() + 1, y + pixel.y() + 1, SlatePlate.BRASS);
    }
    for (DialFace.Pixel pixel : DialFace.needle(value, max, radius)) {
      context.fill(
          x + pixel.x(), y + pixel.y(), x + pixel.x() + 1, y + pixel.y() + 1, NEEDLE_COLOR);
    }
  }
}
