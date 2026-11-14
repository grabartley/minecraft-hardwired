package com.grahambartley.hardwired.screen;

import net.minecraft.client.gui.DrawContext;

public final class SlatePlate {

  public static final int OPAQUE = 0xFF000000;
  public static final int TITLE_COLOR = 0xFFFDF55F;
  public static final int MUTED_COLOR = 0xFFB9B2C4;
  public static final int BRASS_SHADOW = 0xFF8A5A0C;
  public static final int BRASS = 0xFFF5C21C;
  public static final int SLATE = 0xFF2A2632;
  public static final int SLATE_LIGHT = 0xFF3C3746;
  public static final int CALCITE = 0xFFE6E2DA;
  private static final int RIVET = 0xFFBE7E12;
  private static final int RIVET_INSET = 6;
  private static final int RIVET_SIZE = 2;

  private SlatePlate() {}

  public static void draw(DrawContext context, int left, int top, int width, int height) {
    int right = left + width;
    int bottom = top + height;
    context.fill(left, top, right, bottom, BRASS_SHADOW);
    context.fill(left + 1, top + 1, right - 1, bottom - 1, BRASS);
    context.fill(left + 3, top + 3, right - 3, bottom - 3, SLATE);
    for (int x : new int[] {left + RIVET_INSET, right - RIVET_INSET - RIVET_SIZE}) {
      for (int y : new int[] {top + RIVET_INSET, bottom - RIVET_INSET - RIVET_SIZE}) {
        context.fill(x, y, x + RIVET_SIZE, y + RIVET_SIZE, RIVET);
      }
    }
  }
}
