package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.device.IntercomLine;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IntercomSayPayloadTest {

  @ParameterizedTest(name = "{0} characters")
  @ValueSource(ints = {0, 1, IntercomLine.MAX_LENGTH})
  void aLineUpToTheLimitSurvivesTheWire(int length) {
    IntercomSayPayload payload = new IntercomSayPayload(new BlockPos(1, 2, 3), "a".repeat(length));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    IntercomSayPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, IntercomSayPayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} characters")
  @ValueSource(ints = {IntercomLine.MAX_LENGTH + 1, 4096})
  void aLineOverTheLimitIsRefused(int length) {
    IntercomSayPayload payload = new IntercomSayPayload(BlockPos.ORIGIN, "a".repeat(length));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> IntercomSayPayload.CODEC.encode(buffer, payload));
  }
}
