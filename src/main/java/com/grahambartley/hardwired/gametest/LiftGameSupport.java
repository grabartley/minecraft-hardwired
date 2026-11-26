package com.grahambartley.hardwired.gametest;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.block.FuelledSourceBlockEntity;
import com.grahambartley.hardwired.block.LargeLiftBlock;
import com.grahambartley.hardwired.block.LargeLiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlock;
import com.grahambartley.hardwired.block.LiftFloorBlockEntity;
import com.grahambartley.hardwired.block.RedstoneCellBlockEntity;
import com.grahambartley.hardwired.config.LiftSettings;
import com.grahambartley.hardwired.lift.LiftActions;
import com.grahambartley.hardwired.lift.LiftCarEntity;
import com.grahambartley.hardwired.lift.LiftCorner;
import com.grahambartley.hardwired.lift.LiftDial;
import com.grahambartley.hardwired.lift.LiftShaft;
import com.grahambartley.hardwired.network.LiftChoosePayload;
import com.grahambartley.hardwired.registry.ModBlocks;
import com.grahambartley.hardwired.wire.Hue;
import com.grahambartley.hardwired.wire.WireCell;
import com.grahambartley.hardwired.wire.WireEngine;
import com.grahambartley.hardwired.wire.WireFace;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

final class LiftGameSupport {

  private LiftGameSupport() {}

  static final String BURN_BATCH = "lift_burn";
  static final String BATCH = "lift";
  static final int STRONG_CELL = 64;
  static final LiftSettings TEST_PACE = new LiftSettings(6);

  static final String SHAFT = "hardwired:shaft";
  static final int FLOOR_Y = -1;
  static final int LOW = 0;
  static final int MIDDLE = 3;
  static final int HIGH = 6;
  static final int COLUMN_X = 3;
  static final int COLUMN_Z = 3;
  static final int GATE_X = 2;
  static final int PILLAR_X = 1;
  static final int HEAD_Y = 9;
  static final int FULL_CHARGE = 100;
  static final int START_TICK = 5;
  static final int TICKS_FOR_SIX_BLOCKS = 48;
  static final int SIX_BLOCK_RIDE_TICKS = 74;
  static final int AFTER_RIDE_TICKS = 25;
  static final int RIDE_TICK_LIMIT = 300;
  static final int DRY_RIDE_TICK = 30;
  static final int DRY_LANDED_TICKS = 220;
  static final int HALF_SPEED_TICK_SLACK = 2;
  static final int EMPTIED_INTO_RIDE_TICKS = 30;
  static final int TICKS_PER_SECOND = 20;
  static final Identifier GOING_UP = Identifier.of(Hardwired.MOD_ID, "going_up");
  static final BlockPos CELL = new BlockPos(PILLAR_X, FLOOR_Y + 1, 2);
  static final BlockPos HEAD = new BlockPos(COLUMN_X, HEAD_Y, COLUMN_Z);

  static BlockPos gate(int level) {
    return new BlockPos(GATE_X, level, COLUMN_Z);
  }

  static BlockPos inCar(int level) {
    return new BlockPos(COLUMN_X, level, COLUMN_Z);
  }

  static void wire(WireEngine engine, BlockPos pos, Direction side, TestContext context) {
    engine.lay(new WireCell(new WireFace(context.getAbsolutePos(pos), side), Hue.PLAIN));
  }

  static void placeGate(TestContext context, int level) {
    BlockState lower =
        ModBlocks.LIFT_FLOOR
            .getDefaultState()
            .with(LiftFloorBlock.FACING, Direction.EAST)
            .with(LiftFloorBlock.HALF, DoubleBlockHalf.LOWER);
    context.setBlockState(gate(level), lower);
    context.setBlockState(gate(level).up(), lower.with(LiftFloorBlock.HALF, DoubleBlockHalf.UPPER));
  }

  static RedstoneCellBlockEntity build(TestContext context, int charge) {
    return (RedstoneCellBlockEntity) build(context, ModBlocks.REDSTONE_CELL, charge);
  }

  static FuelledSourceBlockEntity build(TestContext context, Block source, int charge) {
    for (int y = FLOOR_Y + 1; y <= HEAD_Y; y++) {
      context.setBlockState(new BlockPos(PILLAR_X, y, COLUMN_Z), Blocks.STONE);
    }
    context.setBlockState(gate(MIDDLE).down(), Blocks.STONE);
    context.setBlockState(gate(HIGH).down(), Blocks.STONE);
    context.setBlockState(new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z), Blocks.STONE);
    placeGate(context, LOW);
    placeGate(context, MIDDLE);
    placeGate(context, HIGH);
    context.setBlockState(HEAD, ModBlocks.LIFT);
    context.setBlockState(CELL, source);
    WireEngine engine = WireEngine.of(context.getWorld());
    for (int y = FLOOR_Y + 1; y <= HEAD_Y; y++) {
      wire(engine, new BlockPos(PILLAR_X, y, COLUMN_Z), Direction.EAST, context);
    }
    wire(engine, new BlockPos(GATE_X, HEAD_Y - 1, COLUMN_Z), Direction.UP, context);
    wire(engine, HEAD, Direction.WEST, context);
    wire(engine, new BlockPos(GATE_X, FLOOR_Y, COLUMN_Z), Direction.UP, context);
    wire(engine, new BlockPos(GATE_X, FLOOR_Y, COLUMN_Z - 1), Direction.UP, context);
    wire(engine, new BlockPos(PILLAR_X, FLOOR_Y, COLUMN_Z - 1), Direction.UP, context);
    FuelledSourceBlockEntity fuelled = context.getBlockEntity(CELL);
    fuelled.setCharge(charge);
    return fuelled;
  }

  static void useGate(TestContext context, ConnectedTestPlayer player, int level) {
    LiftActions.use(player.player(), context.getAbsolutePos(gate(level)));
  }

  static void choose(TestContext context, ConnectedTestPlayer player, int from, int to) {
    LiftActions.choose(
        player.player(),
        new LiftChoosePayload(
            context.getAbsolutePos(gate(from)), context.getAbsolutePos(gate(to)).getY()));
  }

  static LiftShaft shaft(TestContext context) {
    return new LiftShaft(context.getWorld(), context.getAbsolutePos(HEAD));
  }

  static LiftCarEntity car(TestContext context) {
    return shaft(context).car().orElseThrow(() -> new AssertionError("no car"));
  }

  static boolean isOpen(TestContext context, int level) {
    return context.getBlockState(gate(level)).get(LiftFloorBlock.OPEN);
  }

  static double carLevel(TestContext context, LiftCarEntity car) {
    return car.run().level() - context.getAbsolutePos(BlockPos.ORIGIN).getY();
  }

  static void expectInCar(TestContext context, ConnectedTestPlayer rider, int level, String name) {
    ServerPlayerEntity player = rider.player();
    BlockPos spot = context.getAbsolutePos(inCar(level));
    context.assertTrue(player.getBlockPos().equals(spot), name + " at " + player.getBlockPos());
    context.assertTrue(
        MathHelper.approximatelyEquals(player.getY(), spot.getY()),
        name + " feet " + player.getY());
    context.assertFalse(player.isInsideWall(), name + " in a wall");
  }

  static boolean riding(TestContext context, ConnectedTestPlayer rider) {
    return shaft(context)
        .car()
        .filter(car -> car.run().sealed() && car.holds(rider.player()))
        .isPresent();
  }

  static ConnectedTestPlayer riderFrom(TestContext context, int level) {
    return ConnectedTestPlayer.joinAt(context, inCar(level));
  }

  static void finish(TestContext context, ConnectedTestPlayer... players) {
    for (ConnectedTestPlayer player : players) {
      player.leave();
    }
    context.complete();
  }

  static void openAt(TestContext context, ConnectedTestPlayer rider, int level) {
    useGate(context, rider, level);
  }

  static LiftFloorBlockEntity dial(TestContext context, int level) {
    return context.getBlockEntity(gate(level).up());
  }

  static double dialReading(TestContext context, int level) {
    LiftDial dial = dial(context, level).dial();
    return dial.reading(dial.levelAt(context.getWorld().getTime()));
  }

  static void growToLarge(TestContext context) {
    for (LiftCorner corner : LiftCorner.values()) {
      context.setBlockState(
          corner.cell(HEAD),
          ModBlocks.LARGE_LIFT.getDefaultState().with(LargeLiftBlock.CORNER, corner));
    }
    WireEngine.of(context.getWorld()).refresh(context.getAbsolutePos(HEAD));
  }

  static void placeWideGate(TestContext context, int level) {
    BlockState lower =
        ModBlocks.LARGE_LIFT_FLOOR
            .getDefaultState()
            .with(LiftFloorBlock.FACING, Direction.EAST)
            .with(LiftFloorBlock.HALF, DoubleBlockHalf.LOWER);
    BlockState upper = lower.with(LiftFloorBlock.HALF, DoubleBlockHalf.UPPER);
    context.setBlockState(gate(level).up(), Blocks.AIR);
    context.setBlockState(gate(level), Blocks.AIR);
    context.setBlockState(gate(level), lower);
    context.setBlockState(gate(level).up(), upper);
    context.setBlockState(gate(level).south(), lower.with(LargeLiftFloorBlock.SECOND, true));
    context.setBlockState(gate(level).south().up(), upper.with(LargeLiftFloorBlock.SECOND, true));
  }

  static boolean allOpen(TestContext context, int level, boolean open) {
    BlockPos first = gate(level);
    return List.of(first, first.up(), first.south(), first.south().up()).stream()
        .allMatch(cell -> context.getBlockState(cell).get(LiftFloorBlock.OPEN) == open);
  }
}
