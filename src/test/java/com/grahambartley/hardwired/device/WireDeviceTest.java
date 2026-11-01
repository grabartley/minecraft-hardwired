package com.grahambartley.hardwired.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WireDeviceTest {

  @ParameterizedTest(name = "{0} at y {1}")
  @CsvSource({"LOWER, 5", "UPPER, 6"})
  void twoTallListsTheLowerHalfFirst(DoubleBlockHalf half, int y) {
    assertEquals(
        List.of(new BlockPos(1, 5, 1), new BlockPos(1, 6, 1)),
        WireDevice.twoTall(new BlockPos(1, y, 1), half));
  }

  @ParameterizedTest(name = "y {0}")
  @CsvSource({"5", "60"})
  void aPlainDeviceIsItsOwnAnchor(int y) {
    WireDevice device = new WireDevice() {};
    BlockPos pos = new BlockPos(1, y, 1);
    assertEquals(List.of(pos), device.cells(pos, Blocks.STONE.getDefaultState()));
    assertEquals(pos, device.loadAnchor(pos, Blocks.STONE.getDefaultState()));
  }
}
