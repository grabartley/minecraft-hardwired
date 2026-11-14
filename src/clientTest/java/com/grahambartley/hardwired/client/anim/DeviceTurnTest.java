package com.grahambartley.hardwired.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceTurnTest {

  @ParameterizedTest(name = "{0} {1} upright {2} turns x{3} y{4}")
  @CsvSource({
    "FLOOR,NORTH,false,0,0",
    "FLOOR,EAST,false,0,90",
    "FLOOR,SOUTH,false,0,180",
    "FLOOR,WEST,false,0,270",
    "WALL,NORTH,false,90,0",
    "WALL,EAST,false,90,90",
    "WALL,SOUTH,false,90,180",
    "WALL,WEST,false,90,270",
    "CEILING,NORTH,false,180,180",
    "CEILING,EAST,false,180,270",
    "CEILING,SOUTH,false,180,0",
    "CEILING,WEST,false,180,90",
    "FLOOR,NORTH,true,0,0",
    "FLOOR,EAST,true,0,90",
    "FLOOR,SOUTH,true,0,180",
    "FLOOR,WEST,true,0,270",
    "WALL,NORTH,true,0,0",
    "WALL,EAST,true,0,90",
    "WALL,SOUTH,true,0,180",
    "WALL,WEST,true,0,270",
    "CEILING,NORTH,true,180,180",
    "CEILING,EAST,true,180,270",
    "CEILING,SOUTH,true,180,0",
    "CEILING,WEST,true,180,90"
  })
  void turnMatchesTheBlockstateFiles(
      BlockFace face, Direction facing, boolean upright, int x, int y) {
    assertEquals(new DeviceTurn(x, y), DeviceTurn.of(face, facing, upright));
  }
}
