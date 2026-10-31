package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.hardwired.wire.Hue;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PanelMemoryTest {

  private static PanelRow row(int x, boolean on) {
    return new PanelRow(new BlockPos(x, 0, 0), PanelKind.SWITCH, on, Hue.PLAIN, 0, 0, false);
  }

  @Test
  void itRecallsTheLatestReading() {
    PanelMemory memory = new PanelMemory(List.of(row(1, false)));
    memory.remember(row(1, true));
    assertEquals(Optional.of(row(1, true)), memory.recall(row(1, true).pos()));
    assertEquals(1, memory.rows().size());
  }

  @Test
  void aStaleRowIsStoredFresh() {
    PanelMemory memory = new PanelMemory(List.of());
    memory.remember(row(1, true).asStale());
    assertFalse(memory.recall(row(1, true).pos()).orElseThrow().stale());
  }

  @Test
  void rememberReportsAChange() {
    PanelMemory memory = new PanelMemory(List.of());
    assertTrue(memory.remember(row(1, true)));
    assertFalse(memory.remember(row(1, true)));
  }

  @Test
  void forgettingRemovesTheRow() {
    PanelMemory memory = new PanelMemory(List.of(row(1, true)));
    assertTrue(memory.forget(row(1, true).pos()));
    assertFalse(memory.forget(row(1, true).pos()));
    assertEquals(List.of(), memory.rows());
  }

  @ParameterizedTest(name = "{0} extra")
  @ValueSource(ints = {1, 5, 200})
  void theOldestFallsOutPastTheLimit(int extra) {
    PanelMemory memory = new PanelMemory(List.of());
    for (int x = 0; x < PanelRow.MAX_ROWS + extra; x++) {
      memory.remember(row(x, false));
    }
    assertEquals(PanelRow.MAX_ROWS, memory.rows().size());
    assertEquals(Optional.empty(), memory.recall(row(extra - 1, false).pos()));
    assertTrue(memory.recall(row(extra, false).pos()).isPresent());
  }
}
