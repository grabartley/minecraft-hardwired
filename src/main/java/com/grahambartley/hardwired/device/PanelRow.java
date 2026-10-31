package com.grahambartley.hardwired.device;

import com.grahambartley.hardwired.wire.Hue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record PanelRow(
    BlockPos pos, PanelKind kind, boolean on, Hue color, int level, int maxLevel, boolean stale) {

  public static final int MAX_ROWS = 128;

  public static final Codec<PanelRow> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      BlockPos.CODEC.fieldOf("pos").forGetter(PanelRow::pos),
                      PanelKind.CODEC.fieldOf("kind").forGetter(PanelRow::kind),
                      Codec.BOOL.fieldOf("on").forGetter(PanelRow::on),
                      Hue.CODEC.fieldOf("color").forGetter(PanelRow::color),
                      Codec.INT.fieldOf("level").forGetter(PanelRow::level),
                      Codec.INT.fieldOf("max_level").forGetter(PanelRow::maxLevel),
                      Codec.BOOL.fieldOf("stale").forGetter(PanelRow::stale))
                  .apply(instance, PanelRow::new));
  public static final Codec<List<PanelRow>> LIST_CODEC = CODEC.sizeLimitedListOf(MAX_ROWS);

  private static final PacketCodec<RegistryByteBuf, PanelRow> ROW_PACKET_CODEC =
      PacketCodec.ofStatic(
          (buf, row) -> {
            BlockPos.PACKET_CODEC.encode(buf, row.pos());
            PanelKind.PACKET_CODEC.encode(buf, row.kind());
            buf.writeBoolean(row.on());
            Hue.PACKET_CODEC.encode(buf, row.color());
            buf.writeVarInt(row.level());
            buf.writeVarInt(row.maxLevel());
            buf.writeBoolean(row.stale());
          },
          buf ->
              new PanelRow(
                  BlockPos.PACKET_CODEC.decode(buf),
                  PanelKind.PACKET_CODEC.decode(buf),
                  buf.readBoolean(),
                  Hue.PACKET_CODEC.decode(buf),
                  buf.readVarInt(),
                  buf.readVarInt(),
                  buf.readBoolean()));
  public static final PacketCodec<RegistryByteBuf, List<PanelRow>> LIST_PACKET_CODEC =
      ROW_PACKET_CODEC.collect(PacketCodecs.toList(MAX_ROWS));

  public PanelRow asStale() {
    return new PanelRow(pos, kind, on, color, level, maxLevel, true);
  }
}
