package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PanelRowTest {

  private static PanelRow row(PanelKind kind) {
    return new PanelRow(new BlockPos(1, -64, 300), kind, true, Hue.CYAN, 3, 15, false);
  }

  @ParameterizedTest
  @EnumSource(PanelKind.class)
  void aRowSurvivesTheWire(PanelKind kind) {
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    PanelRow.LIST_PACKET_CODEC.encode(buffer, List.of(row(kind)));
    assertEquals(List.of(row(kind)), PanelRow.LIST_PACKET_CODEC.decode(buffer));
  }

  @ParameterizedTest
  @EnumSource(PanelKind.class)
  void aRowSurvivesTheSave(PanelKind kind) {
    assertEquals(
        List.of(row(kind)),
        PanelRow.LIST_CODEC
            .parse(
                NbtOps.INSTANCE,
                PanelRow.LIST_CODEC.encodeStart(NbtOps.INSTANCE, List.of(row(kind))).getOrThrow())
            .getOrThrow());
  }

  @Test
  void aListOverTheLimitIsNeverSent() {
    RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    List<PanelRow> tooMany = Collections.nCopies(PanelRow.MAX_ROWS + 1, row(PanelKind.LAMP));
    assertThrows(RuntimeException.class, () -> PanelRow.LIST_PACKET_CODEC.encode(buffer, tooMany));
  }

  @Test
  void staleOnlyChangesTheMark() {
    PanelRow stale = row(PanelKind.SWITCH).asStale();
    assertTrue(stale.stale());
    assertEquals(row(PanelKind.SWITCH).pos(), stale.pos());
  }
}
