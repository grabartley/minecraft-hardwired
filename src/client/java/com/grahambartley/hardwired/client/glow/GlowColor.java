package com.grahambartley.hardwired.client.glow;

import net.minecraft.util.math.MathHelper;

final class GlowColor {

  private static final float GAIN = 2.2F;
  private static final float MAX_CHANNEL = 255F;
  private static final int BYTE = 0xFF;

  private GlowColor() {}

  static float[] saturated(int rgb) {
    float red = (rgb >> 16 & BYTE) / MAX_CHANNEL;
    float green = (rgb >> 8 & BYTE) / MAX_CHANNEL;
    float blue = (rgb & BYTE) / MAX_CHANNEL;
    float top = Math.max(Math.max(red, green), Math.max(blue, 1F / MAX_CHANNEL));
    return new float[] {red / top, green / top, blue / top};
  }

  static float blend(float hue, float strength, boolean removing) {
    return (removing ? 1F - hue : hue) * strength;
  }

  static float strength(float weight, float daylight) {
    float x = MathHelper.clamp(weight * GAIN, 0F, 1F);
    return x * x * (3F - 2F * x) * (1F - daylight);
  }
}
