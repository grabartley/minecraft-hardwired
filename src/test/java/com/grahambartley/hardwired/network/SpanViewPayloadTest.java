package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.network.SpanViewPayload.SpanView;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireSpan;
import com.grahambartley.hardwired.wire.WireViewer;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SpanViewPayloadTest {

  private static final UUID OWNER = new UUID(7, 7);
  private static final UUID STRANGER = new UUID(8, 8);

  private static WireSpan span(boolean sealed, int age) {
    return new WireSpan(
        new BlockPos(0, 70, 0),
        new BlockPos(24, 72, 3),
        Hue.PLAIN,
        sealed ? OWNER : null,
        6,
        age,
        false);
  }

  @ParameterizedTest(name = "sealed {0} viewer {1} override {2} holding {3}")
  @CsvSource({
    "true,stranger,false,true,false",
    "true,owner,false,true,true",
    "true,stranger,true,true,true",
    "true,owner,false,false,false",
    "false,stranger,false,true,true",
    "false,stranger,false,false,false"
  })
  void stateReachesOnlyAViewerHoldingWireWhoMaySeeTheLine(
      boolean sealed, String who, boolean override, boolean holding, boolean shown) {
    WireViewer viewer = new WireViewer(who.equals("owner") ? OWNER : STRANGER, override);
    SpanView view = SpanView.forViewer(span(sealed, 2), viewer, holding, true);
    assertEquals(shown, view.on());
    assertEquals(2, view.age());
    assertEquals(Hue.PLAIN.ordinal(), view.color());
  }

  @ParameterizedTest(name = "on {0}")
  @ValueSource(booleans = {true, false})
  void aSealedSpanSendsAStrangerOnlyItsShapeColourAndAge(boolean on) {
    SpanView view = SpanView.forViewer(span(true, 3), new WireViewer(STRANGER, false), true, on);
    assertEquals(
        new SpanView(
            span(true, 3).from().asLong(),
            span(true, 3).to().asLong(),
            Hue.PLAIN.ordinal(),
            3,
            false),
        view);
    assertEquals(
        List.of("from", "to", "color", "age", "on"),
        Arrays.stream(SpanView.class.getRecordComponents()).map(c -> c.getName()).toList());
  }

  @ParameterizedTest(name = "{0} spans")
  @ValueSource(ints = {0, 1, 40})
  void spansSurviveTheWire(int count) {
    SpanViewPayload payload =
        new SpanViewPayload(
            IntStream.range(0, count)
                .mapToObj(i -> new SpanView(i, i + 1L, i % 17, i % 4, i % 2 == 0))
                .toList());
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    SpanViewPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, SpanViewPayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} spans")
  @ValueSource(ints = {SpanViewPayload.MOST_SPANS + 1})
  void moreSpansThanTheCapAreRefused(int count) {
    SpanViewPayload payload =
        new SpanViewPayload(
            IntStream.range(0, count).mapToObj(i -> new SpanView(i, i, 0, 0, false)).toList());
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> SpanViewPayload.CODEC.encode(buffer, payload));
  }
}
