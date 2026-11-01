package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

final class GlowColor {

  private static final float GAIN = 2.2F;
  private static final float MAX_CHANNEL = 255F;
  private static final float DARKEST_SKY = 11F;

  private GlowColor() {}

  static float[] channels(int rgb) {
    return new float[] {
      ColorHelper.Argb.getRed(rgb) / MAX_CHANNEL,
      ColorHelper.Argb.getGreen(rgb) / MAX_CHANNEL,
      ColorHelper.Argb.getBlue(rgb) / MAX_CHANNEL
    };
  }

  static float blend(float hue, float strength, boolean removing) {
    return (removing ? 1F - hue : hue) * strength;
  }

  static float strength(float weight, float daylight) {
    float x = MathHelper.clamp(weight * GAIN, 0F, 1F);
    return x * x * (3F - 2F * x) * (1F - daylight);
  }

  static float daylight(int skyLight, int ambientDarkness) {
    return (float) skyLight / Glow.MAX_LEVEL * (1F - ambientDarkness / DARKEST_SKY);
  }
}
