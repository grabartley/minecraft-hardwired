package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CallVoicePayloadTest {

  @ParameterizedTest(name = "{0} bytes")
  @ValueSource(ints = {4, 100, 803})
  void aFrameUpToTheLimitSurvivesTheWire(int bytes) {
    CallVoicePayload payload = new CallVoicePayload(new BlockPos(3, 64, -2), new byte[bytes]);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    CallVoicePayload.CODEC.encode(buffer, payload);
    CallVoicePayload decoded = CallVoicePayload.CODEC.decode(buffer);
    assertEquals(payload.phone(), decoded.phone());
    assertArrayEquals(payload.adpcm(), decoded.adpcm());
  }

  @ParameterizedTest(name = "{0} bytes")
  @ValueSource(ints = {804, 4096})
  void aFrameOverTheLimitIsRefused(int bytes) {
    CallVoicePayload payload = new CallVoicePayload(BlockPos.ORIGIN, new byte[bytes]);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> CallVoicePayload.CODEC.encode(buffer, payload));
  }
}
