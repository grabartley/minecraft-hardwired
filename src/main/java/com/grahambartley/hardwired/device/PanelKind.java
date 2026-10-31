package com.grahambartley.hardwired.device;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

public enum PanelKind implements StringIdentifiable {
  LAMP("lamp"),
  SWITCH("switch"),
  CONTROLLER("controller");

  public static final Codec<PanelKind> CODEC = StringIdentifiable.createCodec(PanelKind::values);
  public static final PacketCodec<RegistryByteBuf, PanelKind> PACKET_CODEC =
      PacketCodecs.indexed(PanelKind::byIndex, PanelKind::ordinal).cast();

  private final String id;

  PanelKind(String id) {
    this.id = id;
  }

  @Override
  public String asString() {
    return id;
  }

  public String translationKey() {
    return "screen.hardwired.main_panel." + id;
  }

  public static PanelKind byIndex(int index) {
    return values()[Math.floorMod(index, values().length)];
  }
}
