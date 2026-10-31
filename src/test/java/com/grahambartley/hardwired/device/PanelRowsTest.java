package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.hardwired.wire.Hue;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PanelRowsTest {

  private static final BlockPos PANEL = BlockPos.ORIGIN;

  private static PanelRow row(PanelKind kind, int x) {
    return new PanelRow(new BlockPos(x, 0, 0), kind, false, Hue.PLAIN, 0, 0, false);
  }

  @Test
  void rowsGroupByKindThenNearestFirst() {
    PanelRow nearLamp = row(PanelKind.LAMP, 1);
    PanelRow farLamp = row(PanelKind.LAMP, 9);
    PanelRow nearController = row(PanelKind.CONTROLLER, 2);
    PanelRow nearSwitch = row(PanelKind.SWITCH, 3);
    assertEquals(
        List.of(nearLamp, farLamp, nearSwitch, nearController),
        PanelRows.ordered(List.of(nearController, farLamp, nearSwitch, nearLamp), PANEL));
  }

  @ParameterizedTest(name = "{0} rows: {1} kept, {2} left out")
  @CsvSource({"0, 0, 0", "128, 128, 0", "129, 128, 1", "500, 128, 372"})
  void theListStaysUnderItsLimit(int size, int kept, int omitted) {
    List<PanelRow> rows = Collections.nCopies(size, row(PanelKind.LAMP, 1));
    assertEquals(kept, PanelRows.capped(rows).size());
    assertEquals(omitted, PanelRows.omitted(rows));
  }
}
