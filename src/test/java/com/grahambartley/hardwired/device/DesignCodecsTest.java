package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.circuit.Design;
import com.grahambartley.hardwired.circuit.DesignText;
import com.grahambartley.hardwired.circuit.Line;
import com.grahambartley.hardwired.circuit.Part;
import com.grahambartley.hardwired.circuit.PartType;
import com.grahambartley.hardwired.circuit.PortMode;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.PacketByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DesignCodecsTest {

  private static final Design DESIGN =
      new Design(
          List.of(PortMode.INPUT, PortMode.OUTPUT, PortMode.UNUSED, PortMode.UNUSED),
          List.of(Part.of(PartType.NOT, 3, 4)),
          List.of(new Line(0, Design.partNode(0), 0), new Line(Design.partNode(0), 1, 0)));

  @Test
  void aDesignRoundTripsThroughTheCodec() {
    var encoded = DesignCodecs.CODEC.encodeStart(JsonOps.INSTANCE, DESIGN).getOrThrow();
    assertEquals(DESIGN, DesignCodecs.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
  }

  @Test
  void aDesignRoundTripsThroughThePacketCodec() {
    PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
    DesignCodecs.PACKET_CODEC.encode(buffer, DESIGN);
    assertEquals(DESIGN, DesignCodecs.PACKET_CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "\"{0}\" is not a design")
  @ValueSource(strings = {"", "HWC1", "HWC1|uuuu|not:1:1:0|4:4:0", "HWC9|uuuu||"})
  void hostileTextFailsTheCodecAndReadsAsEmpty(String text) {
    var json = new com.google.gson.JsonPrimitive(text);
    assertTrue(DesignCodecs.CODEC.parse(JsonOps.INSTANCE, json).isError());
    assertEquals(Design.EMPTY, DesignCodecs.orEmpty(text));
  }

  @Test
  void aPacketWithBadTextReadsAsEmpty() {
    PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
    buffer.writeString("HWC1|uuuu|not:1:1:0|4:4:0", DesignText.MAX_LENGTH);
    assertEquals(Design.EMPTY, DesignCodecs.PACKET_CODEC.decode(buffer));
  }
}
