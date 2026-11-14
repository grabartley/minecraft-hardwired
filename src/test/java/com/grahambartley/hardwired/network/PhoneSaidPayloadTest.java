package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PhoneSaidPayloadTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<PhoneSaidPayload> lines() {
    return Stream.of(
        new PhoneSaidPayload(new BlockPos(3, 64, -2), Text.literal("hello")),
        new PhoneSaidPayload(
            BlockPos.ORIGIN,
            Text.literal("<Alice> ").append(Text.literal("hi").formatted(Formatting.GRAY))),
        new PhoneSaidPayload(
            new BlockPos(-30, 120, 9), Text.translatable("device.hardwired.too_fast")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("lines")
  void aLineSurvivesTheWire(PhoneSaidPayload payload) {
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    PhoneSaidPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, PhoneSaidPayload.CODEC.decode(buffer));
  }
}
