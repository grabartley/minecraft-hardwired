package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeskActionPayloadTest {

  @ParameterizedTest(name = "action {0} value {1}")
  @CsvSource({"0,1000", "4,0", "-1,2147483647"})
  void aPayloadSurvivesTheWire(int action, int value) {
    DeskActionPayload payload = new DeskActionPayload(new BlockPos(1, 2, 3), action, value);
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    DeskActionPayload.CODEC.encode(buf, payload);
    assertEquals(payload, DeskActionPayload.CODEC.decode(buf));
  }
}
