package com.grahambartley.hardwired.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record Scene(String name, LampLook look) {

  public static final int MAX_NAME_LENGTH = 24;
  public static final int MAX_SCENES = 8;
  public static final Codec<Scene> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.string(0, MAX_NAME_LENGTH).fieldOf("name").forGetter(Scene::name),
                      LampLook.CODEC.fieldOf("look").forGetter(Scene::look))
                  .apply(instance, Scene::new));
  public static final Codec<List<Scene>> LIST_CODEC = CODEC.sizeLimitedListOf(MAX_SCENES);
  public static final PacketCodec<RegistryByteBuf, List<Scene>> LIST_PACKET_CODEC =
      PacketCodec.tuple(
              PacketCodecs.string(MAX_NAME_LENGTH),
              Scene::name,
              LampLook.PACKET_CODEC,
              Scene::look,
              Scene::new)
          .collect(PacketCodecs.toList(MAX_SCENES));
}
