package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.device.DeviceStatus;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceInspectResultPayloadTest {

  @ParameterizedTest(name = "{0} of {1}, overloaded {2}, {3}")
  @CsvSource({
    "0,0,false,READY",
    "12,64,false,RUNNING",
    "90,64,true,OVERLOADED",
    "0,0,false,OUT_OF_FUEL"
  })
  void survivesTheWire(int load, int supply, boolean overloaded, DeviceStatus status) {
    DeviceInspectResultPayload payload =
        new DeviceInspectResultPayload(new BlockPos(5, 60, -5), load, supply, overloaded, status);
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    DeviceInspectResultPayload.CODEC.encode(buf, payload);
    assertEquals(payload, DeviceInspectResultPayload.CODEC.decode(buf));
  }
}
