package com.grahambartley.hardwired.wire;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.StringIdentifiable;

public enum Hue implements StringIdentifiable {
  PLAIN("plain", null, 0xE77C56),
  WHITE("white", DyeColor.WHITE, 0xF9FFFE),
  ORANGE("orange", DyeColor.ORANGE, 0xF9801D),
  MAGENTA("magenta", DyeColor.MAGENTA, 0xC74EBD),
  LIGHT_BLUE("light_blue", DyeColor.LIGHT_BLUE, 0x3AB3DA),
  YELLOW("yellow", DyeColor.YELLOW, 0xFED83D),
  LIME("lime", DyeColor.LIME, 0x80C71F),
  PINK("pink", DyeColor.PINK, 0xF38BAA),
  GRAY("gray", DyeColor.GRAY, 0x6B7478),
  LIGHT_GRAY("light_gray", DyeColor.LIGHT_GRAY, 0x9D9D97),
  CYAN("cyan", DyeColor.CYAN, 0x169C9C),
  PURPLE("purple", DyeColor.PURPLE, 0x8932B8),
  BLUE("blue", DyeColor.BLUE, 0x3C44AA),
  BROWN("brown", DyeColor.BROWN, 0x835432),
  GREEN("green", DyeColor.GREEN, 0x5E7C16),
  RED("red", DyeColor.RED, 0xB02E26),
  BLACK("black", DyeColor.BLACK, 0x3A3A44);

  public static final Codec<Hue> CODEC = StringIdentifiable.createCodec(Hue::values);
  public static final PacketCodec<RegistryByteBuf, Hue> PACKET_CODEC =
      PacketCodecs.indexed(Hue::byIndex, Hue::ordinal).cast();
  private static final int WARM_WHITE = 0xFFE6B0;

  private final String id;
  private final DyeColor dye;
  private final int rgb;

  Hue(String id, DyeColor dye, int rgb) {
    this.id = id;
    this.dye = dye;
    this.rgb = rgb;
  }

  @Override
  public String asString() {
    return id;
  }

  public Optional<DyeColor> dye() {
    return Optional.ofNullable(dye);
  }

  public int rgb() {
    return rgb;
  }

  public int lampRgb() {
    return this == PLAIN ? WARM_WHITE : rgb;
  }

  public String wireKey() {
    return "hue.hardwired.wire." + id;
  }

  public String lampKey() {
    return "hue.hardwired.lamp." + id;
  }

  public static Hue byIndex(int index) {
    return values()[Math.floorMod(index, values().length)];
  }

  public static Hue byName(String name) {
    for (Hue hue : values()) {
      if (hue.id.equals(name)) {
        return hue;
      }
    }
    return PLAIN;
  }

  public static Hue ofDye(DyeColor dye) {
    for (Hue hue : values()) {
      if (hue.dye == dye) {
        return hue;
      }
    }
    return PLAIN;
  }
}
