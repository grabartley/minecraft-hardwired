package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceInspectPayloadTest {

  @ParameterizedTest(name = "{0} {1} {2}")
  @CsvSource({"0,0,0", "100,-60,-3000"})
  void survivesTheWire(int x, int y, int z) {
    DeviceInspectPayload payload = new DeviceInspectPayload(new BlockPos(x, y, z));
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    DeviceInspectPayload.CODEC.encode(buf, payload);
    assertEquals(payload, DeviceInspectPayload.CODEC.decode(buf));
  }
}
