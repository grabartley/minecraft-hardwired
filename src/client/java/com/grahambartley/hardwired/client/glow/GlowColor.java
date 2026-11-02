package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

final class GlowColor {

  private static final float GAIN = 2.2F;
  private static final float MAX_CHANNEL = 255F;
  private static final float NIGHT_SKY = 0.2F;
  private static final float SKY_RANGE = 1F - NIGHT_SKY;

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

  static float daylight(int skyLight, float skyBrightness) {
    float day = MathHelper.clamp((skyBrightness - NIGHT_SKY) / SKY_RANGE, 0F, 1F);
    return (float) skyLight / Glow.MAX_LEVEL * day;
  }
}
