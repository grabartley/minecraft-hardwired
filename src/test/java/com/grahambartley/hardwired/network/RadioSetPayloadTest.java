package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RadioSetPayloadTest {

  @ParameterizedTest(name = "on {0} at {1} volume {2}")
  @CsvSource({"true,1000,100", "false,530,0", "true,-5,99999"})
  void aPayloadSurvivesTheWire(boolean on, int frequency, int volume) {
    RadioSetPayload payload = new RadioSetPayload(new BlockPos(1, 2, 3), on, frequency, volume);
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    RadioSetPayload.CODEC.encode(buf, payload);
    assertEquals(payload, RadioSetPayload.CODEC.decode(buf));
  }
}
