package com.grahambartley.hardwired.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MountingTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest(name = "{0} gives {1}")
  @CsvSource({"UP,CEILING", "DOWN,FLOOR", "NORTH,WALL", "EAST,WALL", "SOUTH,WALL", "WEST,WALL"})
  void aClickedSideMountsOnTheMatchingFace(Direction direction, BlockFace face) {
    assertEquals(face, Mounting.faceFor(direction));
  }

  @ParameterizedTest(name = "{0} facing {1} hangs from {2}")
  @CsvSource({
    "FLOOR,NORTH,DOWN",
    "CEILING,EAST,UP",
    "WALL,NORTH,SOUTH",
    "WALL,EAST,WEST",
    "WALL,SOUTH,NORTH",
    "WALL,WEST,EAST"
  })
  void aMountedBlockHangsFromTheSideBehindIt(BlockFace face, Direction facing, Direction attached) {
    assertEquals(attached, Mounting.attachedDirection(face, facing));
  }
}
