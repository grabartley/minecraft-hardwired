package com.grahambartley.hardwired.network;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.wire.WireSpan;
import com.grahambartley.hardwired.wire.WireViewer;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpanViewPayload(List<SpanView> spans) implements CustomPayload {

  public static final int MOST_SPANS = 2048;

  public record SpanView(long from, long to, int color, int age, boolean on) {

    public static SpanView forViewer(
        WireSpan span, WireViewer viewer, boolean holdingWire, boolean on) {
      return new SpanView(
          span.from().asLong(),
          span.to().asLong(),
          span.color().ordinal(),
          span.age(),
          holdingWire && viewer.canSee(span) && on);
    }

    static final PacketCodec<RegistryByteBuf, SpanView> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_LONG.<RegistryByteBuf>cast(),
            SpanView::from,
            PacketCodecs.VAR_LONG.<RegistryByteBuf>cast(),
            SpanView::to,
            PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
            SpanView::color,
            PacketCodecs.VAR_INT.<RegistryByteBuf>cast(),
            SpanView::age,
            PacketCodecs.BOOL.<RegistryByteBuf>cast(),
            SpanView::on,
            SpanView::new);
  }

  public static final Id<SpanViewPayload> ID =
      new Id<>(Identifier.of(Hardwired.MOD_ID, "span_view"));
  public static final PacketCodec<RegistryByteBuf, SpanViewPayload> CODEC =
      PacketCodec.tuple(
          SpanView.CODEC.collect(PacketCodecs.toList(MOST_SPANS)),
          SpanViewPayload::spans,
          SpanViewPayload::new);

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
