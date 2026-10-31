package com.grahambartley.hardwired.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record LampLook(Hue color, int brightness) {

  public static final int MAX_BRIGHTNESS = 15;
  public static final LampLook DEFAULT = new LampLook(Hue.PLAIN, MAX_BRIGHTNESS);
  public static final Codec<LampLook> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Hue.CODEC.fieldOf("color").forGetter(LampLook::color),
                      Codec.INT.fieldOf("brightness").forGetter(LampLook::brightness))
                  .apply(instance, LampLook::new));
  public static final PacketCodec<RegistryByteBuf, LampLook> PACKET_CODEC =
      PacketCodec.tuple(
          Hue.PACKET_CODEC,
          LampLook::color,
          PacketCodecs.VAR_INT,
          LampLook::brightness,
          LampLook::new);

  public LampLook {
    brightness = Math.clamp(brightness, 0, MAX_BRIGHTNESS);
  }
}
