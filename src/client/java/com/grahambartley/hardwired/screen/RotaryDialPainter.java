package com.grahambartley.hardwired.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class RotaryDialPainter {

  static final int FACE_RADIUS = 72;
  static final int DIGIT_RADIUS = 63;
  static final int RING_RADIUS = 42;
  static final int HOLE_RADIUS = 9;
  static final int CARD_RADIUS = 22;

  private static final int CALCITE_SHADE = 0xFFB8B3A9;
  private static final int HOLE = 0xFF1C1A22;
  private static final int CARD = 0xFFF4EEDC;
  private static final int CARD_TEXT = 0xFF3C2A12;
  private static final int STOP_METAL = 0xFF9DA3A8;
  private static final int STOP_SHADE = 0xFF3A3D42;

  private RotaryDialPainter() {}

  static void draw(
      DrawContext context, TextRenderer font, int cx, int cy, double turned, Text number) {
    disk(context, cx, cy, FACE_RADIUS + 1, CALCITE_SHADE);
    disk(context, cx, cy, FACE_RADIUS, SlatePlate.CALCITE);
    for (int pulses = 1; pulses <= RotaryWheel.MOST_PULSES; pulses++) {
      double angle = RotaryWheel.holeAngle(pulses);
      int dx = (int) Math.round(Math.cos(angle) * DIGIT_RADIUS);
      int dy = (int) Math.round(Math.sin(angle) * DIGIT_RADIUS);
      String digit = String.valueOf(RotaryWheel.digitFor(pulses));
      context.drawText(
          font, digit, cx + dx - font.getWidth(digit) / 2, cy + dy - 4, SlatePlate.SLATE, false);
    }
    disk(context, cx, cy, RING_RADIUS + HOLE_RADIUS + 3, SlatePlate.BRASS_SHADOW);
    disk(context, cx, cy, RING_RADIUS + HOLE_RADIUS + 2, SlatePlate.BRASS);
    for (int pulses = 1; pulses <= RotaryWheel.MOST_PULSES; pulses++) {
      double angle = RotaryWheel.holeAngle(pulses) + turned;
      int hx = cx + (int) Math.round(Math.cos(angle) * RING_RADIUS);
      int hy = cy + (int) Math.round(Math.sin(angle) * RING_RADIUS);
      disk(context, hx, hy, HOLE_RADIUS, HOLE);
    }
    disk(context, cx, cy, CARD_RADIUS + 1, SlatePlate.BRASS_SHADOW);
    disk(context, cx, cy, CARD_RADIUS, CARD);
    context.drawText(font, number, cx - font.getWidth(number) / 2, cy - 4, CARD_TEXT, false);
    drawFingerStop(context, cx, cy);
  }

  private static void drawFingerStop(DrawContext context, int cx, int cy) {
    for (int length = RING_RADIUS - HOLE_RADIUS; length <= FACE_RADIUS - 2; length++) {
      DialFace.Pixel pixel = DialFace.onCircle(RotaryWheel.STOP, length);
      int x = cx + pixel.x();
      int y = cy + pixel.y();
      context.fill(x - 2, y - 2, x + 3, y + 3, STOP_SHADE);
      context.fill(x - 1, y - 1, x + 2, y + 2, STOP_METAL);
    }
  }

  private static void disk(DrawContext context, int cx, int cy, int radius, int color) {
    for (int y = -radius; y <= radius; y++) {
      int half = (int) Math.floor(Math.sqrt((double) radius * radius - (double) y * y));
      context.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, color);
    }
  }
}
