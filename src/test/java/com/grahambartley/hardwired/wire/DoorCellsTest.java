package com.grahambartley.hardwired.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DoorCellsTest {

  private static final IntProperty COLUMN = IntProperty.of("column", 0, 9);

  private static Block wideDoor;
  private static Block otherWideDoor;

  private static final class WideDoor extends DoorBlock {
    WideDoor() {
      super(BlockSetType.OAK, AbstractBlock.Settings.create());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      super.appendProperties(builder);
      builder.add(COLUMN);
    }
  }

  @BeforeAll
  static void boot() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
    wideDoor = new WideDoor();
    otherWideDoor = new WideDoor();
  }

  private static BlockState door(Block block, Direction facing, int row) {
    return block
        .getDefaultState()
        .with(DoorBlock.FACING, facing)
        .with(DoorBlock.HALF, row == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
  }

  private static void place(
      Map<BlockPos, BlockState> world,
      Block block,
      Direction facing,
      BlockPos corner,
      int width,
      int height) {
    Direction across = facing.rotateYClockwise();
    for (int column = 0; column < width; column++) {
      for (int row = 0; row < height; row++) {
        world.put(corner.offset(across, column).up(row), door(block, facing, row));
      }
    }
  }

  @ParameterizedTest(name = "a vanilla {0} door at row {1} is its two halves")
  @CsvSource({"NORTH,0", "NORTH,1", "EAST,0", "SOUTH,1"})
  void aVanillaDoorIsItsTwoHalvesEvenBesideAnother(Direction facing, int row) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(0, 64, 0);
    place(world, Blocks.OAK_DOOR, facing, corner, 2, 2);
    BlockPos pos = corner.up(row);
    assertEquals(List.of(corner, corner.up()), DoorCells.of(pos, world.get(pos), world::get));
  }

  @ParameterizedTest(name = "a {0} door {1} wide and {2} tall is one door from any cell")
  @CsvSource({"NORTH,2,2", "NORTH,3,4", "EAST,5,4", "SOUTH,1,6", "WEST,5,6", "NORTH,4,3"})
  void aWideDoorIsOneDoorFromAnyCell(Direction facing, int width, int height) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(10, 70, -3);
    place(world, wideDoor, facing, corner, width, height);
    for (BlockPos pos : world.keySet()) {
      List<BlockPos> cells = DoorCells.of(pos, world.get(pos), world::get);
      assertEquals(corner, cells.getFirst(), "anchor from " + pos);
      assertEquals(world.keySet(), Set.copyOf(cells), "cells from " + pos);
      assertEquals(width * height, cells.size());
    }
  }

  @ParameterizedTest(name = "a {0} wide door stops at a door facing another way")
  @EnumSource(
      value = Direction.class,
      names = {"NORTH", "EAST", "SOUTH", "WEST"})
  void aWideDoorStopsAtADoorFacingAnotherWay(Direction facing) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(0, 64, 0);
    place(world, wideDoor, facing, corner, 3, 2);
    BlockPos beside = corner.offset(facing.rotateYClockwise(), 3);
    place(world, wideDoor, facing.getOpposite(), beside, 1, 2);
    assertEquals(6, DoorCells.of(corner, world.get(corner), world::get).size());
    assertEquals(2, DoorCells.of(beside, world.get(beside), world::get).size());
  }

  @ParameterizedTest(name = "a wide door stops at a different door block, width {0}")
  @CsvSource({"1", "3"})
  void aWideDoorStopsAtADifferentDoorBlock(int width) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(0, 64, 0);
    place(world, wideDoor, Direction.NORTH, corner, width, 2);
    BlockPos beside = corner.offset(Direction.NORTH.rotateYClockwise(), width);
    place(world, otherWideDoor, Direction.NORTH, beside, 2, 2);
    assertEquals(width * 2, DoorCells.of(corner, world.get(corner), world::get).size());
  }

  @ParameterizedTest(name = "an unloaded cell ends the door at height {0}")
  @CsvSource({"3", "5"})
  void anUnloadedCellEndsTheDoor(int height) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(0, 64, 0);
    place(world, wideDoor, Direction.NORTH, corner, 1, height);
    world.remove(corner.up(1));
    assertEquals(List.of(corner), DoorCells.of(corner, world.get(corner), world::get));
  }

  @ParameterizedTest(name = "a {0} door wall {1} wide stops at the cap with one anchor")
  @CsvSource({"NORTH,10", "EAST,20", "SOUTH,12"})
  void aHugeRunOfDoorCellsStopsAtTheCapWithOneAnchor(Direction facing, int size) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    place(world, wideDoor, facing, BlockPos.ORIGIN, size, size);
    for (BlockPos pos : world.keySet()) {
      List<BlockPos> cells = DoorCells.of(pos, world.get(pos), world::get);
      assertEquals(BlockPos.ORIGIN, cells.getFirst(), "anchor from " + pos);
      assertTrue(cells.contains(pos), "keeps the touched cell " + pos);
      assertTrue(cells.size() <= DoorCells.MAX_CELLS + 1, "capped from " + pos);
    }
  }

  @ParameterizedTest(name = "two {0} wide doors of one block side by side count as one door")
  @CsvSource({"1", "3"})
  void twoWideDoorsOfOneBlockSideBySideCountAsOne(int width) {
    Map<BlockPos, BlockState> world = new HashMap<>();
    BlockPos corner = new BlockPos(0, 64, 0);
    place(world, wideDoor, Direction.NORTH, corner, width, 2);
    place(
        world,
        wideDoor,
        Direction.NORTH,
        corner.offset(Direction.NORTH.rotateYClockwise(), width),
        width,
        2);
    assertEquals(4 * width, DoorCells.of(corner, world.get(corner), world::get).size());
  }

  @ParameterizedTest(name = "fits: {0}")
  @CsvSource({"oak_door,true", "wide_door,true", "lever,false", "tall_grass,false"})
  void onlyStatesWithADoorFacingAndHalfFit(String name, boolean fits) {
    BlockState state =
        switch (name) {
          case "oak_door" -> Blocks.OAK_DOOR.getDefaultState();
          case "wide_door" -> wideDoor.getDefaultState();
          case "lever" -> Blocks.LEVER.getDefaultState();
          default -> Blocks.TALL_GRASS.getDefaultState();
        };
    assertEquals(fits, DoorCells.fits(state));
  }
}
