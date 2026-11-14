package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.phone.PartyLines;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExchangeSharePayloadTest {

  private static List<BlockPos> phones(int count) {
    return IntStream.range(0, count).mapToObj(i -> new BlockPos(i, 64, -i)).toList();
  }

  @ParameterizedTest(name = "{0} phones share {1}")
  @CsvSource({"0,true", "2,true", "4,false"})
  void aChoiceSurvivesTheWire(int count, boolean share) {
    ExchangeSharePayload payload =
        new ExchangeSharePayload(new BlockPos(8, 70, 3), phones(count), share);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    ExchangeSharePayload.CODEC.encode(buffer, payload);
    assertEquals(payload, ExchangeSharePayload.CODEC.decode(buffer));
  }

  @ParameterizedTest(name = "{0} phones")
  @ValueSource(ints = {PartyLines.MOST_PHONES + 1, 64})
  void moreThanAPartyLineHoldsIsRefused(int count) {
    ExchangeSharePayload payload = new ExchangeSharePayload(BlockPos.ORIGIN, phones(count), true);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> ExchangeSharePayload.CODEC.encode(buffer, payload));
  }
}
