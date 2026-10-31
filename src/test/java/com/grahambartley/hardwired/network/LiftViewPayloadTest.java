package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.lift.LiftFloors;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LiftViewPayloadTest {

  private static List<Integer> floors(int count) {
    return IntStream.range(-count / 2, count - count / 2).boxed().toList();
  }

  @ParameterizedTest(name = "{0} floors")
  @ValueSource(ints = {0, 1, 3, LiftFloors.MAX_FLOORS})
  void aFloorListUpToTheCapSurvivesTheWire(int count) {
    LiftViewPayload payload = new LiftViewPayload(new BlockPos(1, 2, 3), floors(count), -1);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    LiftViewPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, LiftViewPayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} floors")
  @ValueSource(ints = {LiftFloors.MAX_FLOORS + 1, 4096})
  void aFloorListOverTheCapIsRefused(int count) {
    LiftViewPayload payload = new LiftViewPayload(BlockPos.ORIGIN, floors(count), 0);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> LiftViewPayload.CODEC.encode(buffer, payload));
  }
}
