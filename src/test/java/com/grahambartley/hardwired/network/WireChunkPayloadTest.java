package com.grahambartley.hardwired.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.network.WireChunkPayload.MarkView;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class WireChunkPayloadTest {

  @ParameterizedTest(name = "{0}")
  @EnumSource(Direction.class)
  void anArmSurvivesTheMaskAndNoOtherAppears(Direction arm) {
    CellView view = new CellView(0L, 0, 0, 0, CellView.armMask(Set.of(arm)));
    assertEquals(
        EnumSet.of(arm),
        EnumSet.copyOf(Arrays.stream(Direction.values()).filter(view::hasArm).toList()));
  }

  @ParameterizedTest(name = "flags {0}")
  @ValueSource(ints = {0, WireChunkPayload.ON, WireChunkPayload.LOOSE_END})
  void onlyTheSealedFlagMarksACellSealed(int flags) {
    assertEquals(false, new CellView(0L, 0, 0, flags, 0).isSealed());
    assertEquals(true, new CellView(0L, 0, 0, flags | WireChunkPayload.SEALED, 0).isSealed());
  }

  @ParameterizedTest(name = "arms {0}")
  @ValueSource(ints = {0, 1, 12, 60})
  void aChunkSurvivesTheWire(int arms) {
    WireChunkPayload payload =
        new WireChunkPayload(
            7L,
            List.of(new CellView(5L, 1, 2, WireChunkPayload.SEALED, arms)),
            List.of(new MarkView(6L, true)));
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), null);
    WireChunkPayload.CODEC.encode(buffer, payload);
    assertEquals(payload, WireChunkPayload.CODEC.decode(buffer));
  }
}
