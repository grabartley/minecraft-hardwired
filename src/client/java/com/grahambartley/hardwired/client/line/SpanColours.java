package com.grahambartley.hardwired.client.line;

import com.grahambartley.hardwired.wire.Hue;

public final class SpanColours {

  private static final int[] COPPER_AGES = {0xC46D51, 0xA1806A, 0x6C9A6E, 0x52A284};
  private static final float ON_LIFT = 0.35F;

  private SpanColours() {}

  public static int rgb(int color, int age, boolean on) {
    Hue hue = Hue.byIndex(color);
    int base =
        hue == Hue.PLAIN ? COPPER_AGES[Math.clamp(age, 0, COPPER_AGES.length - 1)] : hue.rgb();
    return on ? lift(base) : base;
  }

  private static int lift(int rgb) {
    int red = channel(rgb >> 16);
    int green = channel(rgb >> 8);
    int blue = channel(rgb);
    return red << 16 | green << 8 | blue;
  }

  private static int channel(int value) {
    int part = value & 0xFF;
    return Math.round(part + (0xFF - part) * ON_LIFT);
  }
}
