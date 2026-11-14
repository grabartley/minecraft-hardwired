package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.hardwired.phone.PartyLines;
import com.grahambartley.hardwired.phone.PhoneEntry;
import com.grahambartley.hardwired.phone.PhoneNumbering;
import io.netty.buffer.Unpooled;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ExchangeViewPayloadTest {

  private static final int MOST_LINES = PhoneNumbering.LINES_PER_EXCHANGE * PartyLines.MOST_PHONES;

  private static ExchangeViewPayload view(int lines, int nameLength) {
    return new ExchangeViewPayload(
        new BlockPos(2, 65, 9),
        3,
        IntStream.range(0, lines)
            .mapToObj(
                i ->
                    new ExchangeViewPayload.Line(
                        new BlockPos(i, 64, 0), 300 + i % 100, "n".repeat(nameLength)))
            .toList());
  }

  @ParameterizedTest(name = "{0} lines named {1} long")
  @CsvSource({"0,0", "1,8", "40,50", "400,50"})
  void aBoardSurvivesTheWire(int lines, int nameLength) {
    ExchangeViewPayload payload = view(lines, nameLength);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    ExchangeViewPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, ExchangeViewPayload.CODEC.decode(buffer));
  }

  static Stream<Arguments> overLimits() {
    return Stream.of(Arguments.of(MOST_LINES + 1, 1), Arguments.of(1, PhoneEntry.MAX_NAME + 1));
  }

  @ParameterizedTest(name = "{0} lines named {1} long")
  @MethodSource("overLimits")
  void aBoardOverTheLimitsIsRefused(int lines, int nameLength) {
    ExchangeViewPayload payload = view(lines, nameLength);
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    assertThrows(RuntimeException.class, () -> ExchangeViewPayload.CODEC.encode(buffer, payload));
  }
}
