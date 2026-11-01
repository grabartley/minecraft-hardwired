package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceInspectResultPayloadTest {

  @ParameterizedTest(name = "{0} of {1}, overloaded {2}")
  @CsvSource({"0,0,false", "12,64,false", "90,64,true"})
  void survivesTheWire(int load, int supply, boolean overloaded) {
    DeviceInspectResultPayload payload =
        new DeviceInspectResultPayload(new BlockPos(5, 60, -5), load, supply, overloaded);
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    DeviceInspectResultPayload.CODEC.encode(buf, payload);
    assertEquals(payload, DeviceInspectResultPayload.CODEC.decode(buf));
  }
}
