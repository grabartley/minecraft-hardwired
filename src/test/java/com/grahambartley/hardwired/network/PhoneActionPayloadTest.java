package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.device.IntercomLine;
import io.netty.buffer.Unpooled;
import java.util.stream.Stream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneActionPayloadTest {

  static Stream<PhoneActionPayload> actions() {
    BlockPos phone = new BlockPos(4, 70, -9);
    return Stream.of(
        PhoneActionPayload.dial(phone, 105),
        PhoneActionPayload.hangUp(phone),
        PhoneActionPayload.say(phone, "a".repeat(IntercomLine.MAX_LENGTH)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("actions")
  void everyActionSurvivesTheWire(PhoneActionPayload payload) {
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    PhoneActionPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, PhoneActionPayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} characters")
  @ValueSource(ints = {IntercomLine.MAX_LENGTH + 1, 4096})
  void aLineOverTheLimitIsRefused(int length) {
    PhoneActionPayload payload = PhoneActionPayload.say(BlockPos.ORIGIN, "a".repeat(length));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> PhoneActionPayload.CODEC.encode(buffer, payload));
  }
}
