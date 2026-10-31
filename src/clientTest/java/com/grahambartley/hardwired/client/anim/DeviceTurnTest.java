package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceTurnTest {

  @ParameterizedTest(name = "{0} {1} turns x{2} y{3}")
  @CsvSource({
    "FLOOR,NORTH,0,0",
    "FLOOR,EAST,0,90",
    "FLOOR,SOUTH,0,180",
    "FLOOR,WEST,0,270",
    "WALL,NORTH,90,0",
    "WALL,EAST,90,90",
    "WALL,SOUTH,90,180",
    "WALL,WEST,90,270",
    "CEILING,NORTH,180,180",
    "CEILING,EAST,180,270",
    "CEILING,SOUTH,180,0",
    "CEILING,WEST,180,90"
  })
  void turnMatchesTheBlockstateFiles(BlockFace face, Direction facing, int x, int y) {
    assertEquals(new DeviceTurn(x, y), DeviceTurn.of(face, facing));
  }
}
