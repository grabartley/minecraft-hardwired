package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HumLevelPayloadTest {

  @ParameterizedTest(name = "load {0}")
  @ValueSource(ints = {0, 1, 128, 4096})
  void survivesTheWire(int load) {
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    HumLevelPayload.CODEC.encode(buf, new HumLevelPayload(load));
    assertEquals(new HumLevelPayload(load), HumLevelPayload.CODEC.decode(buf));
  }
}
