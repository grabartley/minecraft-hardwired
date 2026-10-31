package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

class WireGridNbtTest {

  private static WireCell cell(int x, Hue hue) {
    return new WireCell(new WireFace(new BlockPos(x, 70, -3), Direction.NORTH), hue);
  }

  @Test
  void everyFieldSurvivesARoundTrip() {
    WireGrid grid = new WireGrid();
    WireNetwork red = grid.add(cell(0, Hue.RED));
    grid.add(cell(1, Hue.RED));
    red.setOn(true);
    red.setTripped(true);
    red.setLook(new LampLook(Hue.CYAN, 9));
    red.rememberedInputs().add(1234L);
    grid.add(cell(5, Hue.BLUE));

    WireGrid read = WireGridNbt.read(WireGridNbt.write(grid));

    assertEquals(2, read.networks().size());
    WireNetwork restored = read.networkOf(cell(1, Hue.RED));
    assertEquals(2, restored.size());
    assertTrue(restored.isOn());
    assertTrue(restored.isTripped());
    assertEquals(new LampLook(Hue.CYAN, 9), restored.look());
    assertTrue(restored.rememberedInputs().contains(1234L));
    assertEquals(List.of(), read.drainDirty());
  }
}
