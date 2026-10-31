package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.config.CircuitSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public final class DesignCodecs {

  public static final Codec<Design> CODEC =
      Codec.STRING.comapFlatMap(DesignCodecs::decode, DesignText::encode);
  public static final PacketCodec<ByteBuf, Design> PACKET_CODEC =
      PacketCodecs.string(DesignText.MAX_LENGTH).xmap(DesignCodecs::orEmpty, DesignText::encode);

  private DesignCodecs() {}

  private static DataResult<Design> decode(String text) {
    return switch (DesignText.parse(text, CircuitSettings.MAX_PARTS_MAX)) {
      case DesignText.Result.Accepted accepted -> DataResult.success(accepted.design());
      case DesignText.Result.Refused refused ->
          DataResult.error(() -> "bad circuit design: " + refused.reason());
    };
  }

  public static Design orEmpty(String text) {
    return decode(text).result().orElse(Design.EMPTY);
  }
}
