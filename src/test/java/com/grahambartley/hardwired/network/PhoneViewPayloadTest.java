package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.phone.PhoneDirectory.Listing;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneViewPayloadTest {

  private static List<Listing> listings(int count) {
    return IntStream.range(0, count).mapToObj(i -> new Listing(100 + i, "n" + i)).toList();
  }

  @ParameterizedTest(name = "{0} listings")
  @ValueSource(ints = {0, 1, PhoneViewPayload.MAX_LISTINGS})
  void aDirectoryUpToAFullNetworkSurvivesTheWire(int count) {
    PhoneViewPayload payload = new PhoneViewPayload(new BlockPos(1, 2, 3), 105, 2, listings(count));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    PhoneViewPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, PhoneViewPayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} characters")
  @ValueSource(ints = {PhoneViewPayload.MAX_NAME + 1, 400})
  void aNameOverTheLimitIsRefused(int length) {
    PhoneViewPayload payload =
        new PhoneViewPayload(
            BlockPos.ORIGIN, 100, 0, List.of(new Listing(100, "a".repeat(length))));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> PhoneViewPayload.CODEC.encode(buffer, payload));
  }
}
