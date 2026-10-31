package com.grahambartley.hardwired.screen;

import net.minecraft.client.gui.DrawContext;

public final class PhosphorScreen {

  public static final int CASE = 0xFFD9CFB2;
  public static final int CASE_LIGHT = 0xFFEDE6D0;
  public static final int CASE_SHADOW = 0xFF9C9276;
  public static final int INK = 0xFF3A3528;
  public static final int GLASS = 0xFF06140A;
  public static final int GRID_DOT = 0xFF11361C;
  public static final int DIM = 0xFF1F8A3E;
  public static final int BRIGHT = 0xFF4DFF7C;
  public static final int WARNING = 0xFFFFB347;

  private PhosphorScreen() {}

  public static void drawCase(DrawContext context, int left, int top, int width, int height) {
    int right = left + width;
    int bottom = top + height;
    context.fill(left, top, right, bottom, CASE_SHADOW);
    context.fill(left, top, right - 1, bottom - 1, CASE_LIGHT);
    context.fill(left + 1, top + 1, right - 1, bottom - 1, CASE);
  }

  public static void drawGlass(DrawContext context, int left, int top, int right, int bottom) {
    context.fill(left - 1, top - 1, right + 1, bottom + 1, CASE_SHADOW);
    context.fill(left, top, right, bottom, GLASS);
  }
}
