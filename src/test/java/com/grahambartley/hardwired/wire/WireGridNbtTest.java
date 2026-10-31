package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WireGridNbtTest {

  private static WireCell cell(int x, Hue hue) {
    return new WireCell(new WireFace(new BlockPos(x, 70, -3), Direction.NORTH), hue);
  }

  @ParameterizedTest
  @EnumSource(Hue.class)
  void everyFieldSurvivesARoundTrip(Hue hue) {
    WireGrid grid = new WireGrid();
    WireNetwork red = grid.add(cell(0, hue));
    grid.add(cell(1, hue));
    red.setOn(true);
    red.setTripped(true);
    red.setLook(new LampLook(Hue.CYAN, 9));
    red.rememberedInputs().add(1234L);
    grid.add(cell(5, hue == Hue.BLUE ? Hue.RED : Hue.BLUE));

    WireGrid read = WireGridNbt.read(WireGridNbt.write(grid));

    assertEquals(2, read.networks().size());
    WireNetwork restored = read.networkOf(cell(1, hue));
    assertEquals(2, restored.size());
    assertTrue(restored.isOn());
    assertTrue(restored.isTripped());
    assertEquals(new LampLook(Hue.CYAN, 9), restored.look());
    assertTrue(restored.rememberedInputs().contains(1234L));
    assertEquals(List.of(), read.drainDirty());
  }
}
