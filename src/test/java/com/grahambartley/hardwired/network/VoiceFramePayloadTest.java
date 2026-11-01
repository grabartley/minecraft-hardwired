package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VoiceFramePayloadTest {

  @ParameterizedTest(name = "{0} bytes")
  @ValueSource(ints = {0, 1, 803})
  void aFrameUpToTheMostAMicSendsReachesTheHandlerToBeJudged(int bytes) {
    byte[] adpcm = new byte[bytes];
    if (bytes > 0) {
      adpcm[bytes - 1] = 7;
    }
    VoiceFramePayload payload = new VoiceFramePayload(new BlockPos(1, 2, 3), adpcm);
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    VoiceFramePayload.CODEC.encode(buf, payload);
    VoiceFramePayload decoded = VoiceFramePayload.CODEC.decode(buf);
    assertEquals(payload.desk(), decoded.desk());
    assertArrayEquals(adpcm, decoded.adpcm());
  }

  @ParameterizedTest(name = "{0} bytes")
  @ValueSource(ints = {804, 5000, 32767})
  void aLongerFrameIsNotRead(int bytes) {
    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), null);
    BlockPos.PACKET_CODEC.encode(buf, new BlockPos(1, 2, 3));
    PacketCodecs.byteArray(Short.MAX_VALUE).encode(buf, new byte[bytes]);
    assertThrows(DecoderException.class, () -> VoiceFramePayload.CODEC.decode(buf));
  }
}
