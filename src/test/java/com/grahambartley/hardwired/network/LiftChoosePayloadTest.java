package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LiftChoosePayloadTest {

  @ParameterizedTest(name = "level {0}")
  @ValueSource(ints = {-64, 0, 319})
  void aChoiceSurvivesTheWire(int level) {
    LiftChoosePayload payload = new LiftChoosePayload(new BlockPos(1, 2, 3), level);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    LiftChoosePayload.CODEC.encode(buffer, payload);
    assertEquals(payload, LiftChoosePayload.CODEC.decode(buffer));
  }
}
